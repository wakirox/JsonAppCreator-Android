package it.sapienza.appinterpreter.runtime

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.sapienza.appinterpreter.alerts.AlertsViewModel
import it.sapienza.appinterpreter.calendar.CalendarEntry
import it.sapienza.appinterpreter.calendar.CreateCalendarEventUseCase
import it.sapienza.appinterpreter.json.getObject
import it.sapienza.appinterpreter.json.getValue
import it.sapienza.appinterpreter.json.matchReplace
import it.sapienza.appinterpreter.model.ModelApplication
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.event.AlertMessage
import it.sapienza.appinterpreter.model.event.CalendarEvent
import it.sapienza.appinterpreter.model.event.Event
import it.sapienza.appinterpreter.model.event.OpenLink
import it.sapienza.appinterpreter.model.event.ShareText
import it.sapienza.appinterpreter.model.event.RESTService
import it.sapienza.appinterpreter.model.event.ShowView
import it.sapienza.appinterpreter.model.view_model.FormView
import it.sapienza.appinterpreter.model.view_model.helper.MView
import it.sapienza.appinterpreter.runtime.usecase.FetchJsonUseCase
import it.sapienza.appinterpreter.runtime.usecase.FindMissingApiKeysUseCase
import it.sapienza.appinterpreter.runtime.usecase.InitialFormValuesUseCase
import it.sapienza.appinterpreter.runtime.usecase.LoadListItemsUseCase
import it.sapienza.appinterpreter.runtime.usecase.LoadScreenUseCase
import it.sapienza.appinterpreter.runtime.usecase.MissingValueException
import it.sapienza.appinterpreter.runtime.usecase.OpenLinkUseCase
import it.sapienza.appinterpreter.runtime.usecase.SubmitFormUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

/**
 * One screen of a JSON app. It loads the screen, keeps its forms ([FormsState]) and lists ([ListsState]), and runs
 * its events; the use cases (Koin provides them on Android, see AppModule) do the work. It outlives the screen's
 * recreation, and leaving the screen cancels its requests, which run in [viewModelScope]. Its state is plain
 * StateFlows, for Compose on Android or SwiftUI on iOS.
 *
 * [saved] starts with the screen's arguments: [EXTRA_VIEW_ID] names the view to show (none: the app's main view),
 * and [EXTRA_SCREEN_PAYLOAD] holds its data.
 */
class ScreenViewModel(
    saved: SavedStateHandle,
    val alerts: AlertsViewModel,
    private val fetchJson: FetchJsonUseCase,
    loadListItems: LoadListItemsUseCase,
    initialFormValues: InitialFormValuesUseCase,
    submitForm: SubmitFormUseCase,
    private val createCalendarEvent: CreateCalendarEventUseCase,
    private val openLink: OpenLinkUseCase,
    findMissingApiKeys: FindMissingApiKeysUseCase,
    loadScreen: LoadScreenUseCase,
    private val strings: RuntimeStrings,
) : ViewModel() {

    /** One-off things only the platform's screen can do. */
    sealed interface Effect {
        /** Open [view] with [payload]; [title], if any, replaces the app's name in its app bar. */
        class OpenScreen(val view: MView, val payload: JsonObject?, val title: String? = null) : Effect
        class AddToCalendar(val entry: CalendarEntry) : Effect
        /** Hand [url] to the app for its [scheme]: https/http, geo, tel or mailto (see [OpenLinkUseCase]). */
        class OpenUrl(val url: String, val scheme: String) : Effect
        class Share(val text: String) : Effect
    }

    private val effectChannel = Channel<Effect>(Channel.BUFFERED)
    val effects: Flow<Effect> = effectChannel.receiveAsFlow()

    private val loaded = loadScreen(saved[EXTRA_VIEW_ID], saved[EXTRA_SCREEN_PAYLOAD])
        .also { if (it is LoadScreenUseCase.Result.Failed) alerts.show(it.message) }

    /** The parsed app, or null when it can't be shown (an alert says why). */
    val app: ModelApplication? = (loaded as? LoadScreenUseCase.Result.Ready)?.app

    /** The title the event that opened this screen gave it, shown instead of the app's name. */
    val title: String? = saved[EXTRA_SCREEN_TITLE]

    /** What this screen shows, or null when it can't be shown (an alert says why). */
    val screen: ScreenRuntimeState? = (loaded as? LoadScreenUseCase.Result.Ready)?.screen

    val forms = FormsState(saved, initialFormValues, submitForm)
    val lists = ListsState(viewModelScope, loadListItems)

    private val missing = MutableStateFlow(screen?.let { findMissingApiKeys() })

    /** The app's API keys this phone lacks: asked for before the screen shows, unless skipped. */
    val missingKeys: StateFlow<FindMissingApiKeysUseCase.Missing?> = missing.asStateFlow()

    /** The keys stay missing: the app opens without them, and what needs them can't load. */
    fun skipKeys() {
        missing.value = null
    }

    private val busy = MutableStateFlow(false)

    /** True while a request started by an event is running. */
    val loading: StateFlow<Boolean> = busy.asStateFlow()
    private var requests = 0

    /** [view] as this app defines it, following a reference to a reusable view. */
    fun resolve(view: MView): MView = app?.viewBy(view) ?: view

    /** [action] as this app defines it, following a reference to a reusable action. */
    fun resolve(action: Action?): Action? = action?.let { app?.actionBy(it) }

    /** Runs [action]'s event with the action's own data, or [data] when it has none. */
    fun onAction(action: Action?, data: JsonObject?) {
        val resolved = resolve(action) ?: return
        resolved.event?.let { onEvent(it, resolved.data ?: data) }
    }

    /** Sends [form] to its action's event, once every required field is filled in. */
    fun onSubmit(form: FormView, data: JsonObject?) {
        val json = forms.submit(form, data) ?: return
        resolve(form.action)?.event?.let { onEvent(it, json) }
    }

    fun onEvent(event: Event, data: JsonObject?) {
        when (event) {
            is AlertMessage -> showAlert(event, data)
            // The view receives the part of the data the mapping selects, such as "meals.0".
            is ShowView -> openScreen(event, event.mapping?.let { data?.getObject(it) } ?: data)
            is RESTService -> request(event, data)
            is CalendarEvent -> when (val result = createCalendarEvent(event, event.mapping?.let { data?.getObject(it) } ?: data)) {
                is CreateCalendarEventUseCase.Result.Ready -> effectChannel.trySend(Effect.AddToCalendar(result.entry))
                is CreateCalendarEventUseCase.Result.BadDate -> alerts.show(result.message)
            }
            is OpenLink -> when (val result = openLink(event, event.mapping?.let { data?.getObject(it) } ?: data)) {
                is OpenLinkUseCase.Result.Ready -> effectChannel.trySend(Effect.OpenUrl(result.url, result.scheme))
                is OpenLinkUseCase.Result.Missing -> alerts.show(strings.valueMissing(result.path))
                is OpenLinkUseCase.Result.NotAllowed -> alerts.show(strings.linkNotAllowed(result.url))
            }
            is ShareText -> {
                val shared = event.mapping?.let { data?.getObject(it) } ?: data
                event.text.matchReplace(shared).takeIf { it.isNotBlank() }?.let { effectChannel.trySend(Effect.Share(it)) }
            }
        }
    }

    private fun showAlert(event: AlertMessage, data: JsonObject?) {
        val message = (event.mapping?.let { data?.getValue(it) } ?: event.value)?.matchReplace(data)
        val ok = event.thenDoOK?.let { next -> { onEvent(next, data) } }
        // An alert that does something on OK can always be declined: Cancel then just closes it.
        val cancel = event.thenDoKO?.let { next -> { onEvent(next, data) } } ?: if (ok != null) ({ }) else null
        alerts.show(message ?: "Missing message (check model)", ok, cancel)
    }

    private fun openScreen(event: ShowView, payload: JsonObject?) {
        val definition = app?.viewBy(event.view) ?: return alerts.show(strings.viewMissing(event.view.id.orEmpty()))
        val data = payload ?: definition.data
        val title = event.title?.matchReplace(data)?.trim()?.takeIf { it.isNotEmpty() }
        effectChannel.trySend(Effect.OpenScreen(definition, data, title))
    }

    private fun request(event: RESTService, data: JsonObject?) {
        viewModelScope.launch {
            requests += 1
            busy.value = true
            try {
                val response = fetchJson(event, data)
                event.thenDo?.let { onEvent(it, response) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: MissingValueException) {
                alerts.show(strings.valueMissing(e.path))
            } catch (e: Exception) {
                alerts.show(e.message ?: "An error occurred")
            } finally {
                requests -= 1
                busy.value = requests > 0
            }
        }
    }

    companion object {
        const val EXTRA_VIEW_ID = "idView"
        const val EXTRA_SCREEN_PAYLOAD = "screenPayload"
        const val EXTRA_SCREEN_TITLE = "screenTitle"
    }
}
