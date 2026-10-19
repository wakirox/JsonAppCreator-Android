package it.sapienza.appinterpreter.runtime

import it.sapienza.appinterpreter.model.dataObj
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.alerts.AlertsViewModel
import it.sapienza.appinterpreter.extensions.getObject
import it.sapienza.appinterpreter.extensions.getValue
import it.sapienza.appinterpreter.extensions.matchReplace
import it.sapienza.appinterpreter.model.ModelApplication
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.event.AlertMessage
import it.sapienza.appinterpreter.model.event.CalendarEvent
import it.sapienza.appinterpreter.model.event.Event
import it.sapienza.appinterpreter.model.event.RESTService
import it.sapienza.appinterpreter.model.event.ShowView
import it.sapienza.appinterpreter.model.view_model.FormView
import it.sapienza.appinterpreter.model.view_model.ListView
import it.sapienza.appinterpreter.model.view_model.helper.MView
import it.sapienza.appinterpreter.calendar.CalendarEntry
import it.sapienza.appinterpreter.calendar.CreateCalendarEventUseCase
import it.sapienza.appinterpreter.model.toJsonObject
import it.sapienza.appinterpreter.runtime.usecase.FetchJsonUseCase
import it.sapienza.appinterpreter.runtime.usecase.FindMissingApiKeysUseCase
import it.sapienza.appinterpreter.runtime.usecase.InitialFormValuesUseCase
import it.sapienza.appinterpreter.runtime.usecase.LoadListItemsUseCase
import it.sapienza.appinterpreter.runtime.usecase.LoadScreenUseCase
import it.sapienza.appinterpreter.runtime.usecase.SubmitFormUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * One screen of a JSON app. It loads the screen, keeps its forms ([FormsState]) and lists ([ListsState]), and runs
 * its events; the use cases, which Koin provides (see AppModule), do the work. It outlives the activity's recreation, and leaving the screen cancels its
 * requests, which run in [viewModelScope].
 *
 * [saved] starts with the activity's intent extras: [EXTRA_VIEW_ID] names the view to show (none: the app's main
 * view), and [EXTRA_SCREEN_PAYLOAD] holds its data.
 */
class ScreenViewModel(
    application: Application,
    saved: SavedStateHandle,
    val alerts: AlertsViewModel,
    private val fetchJson: FetchJsonUseCase,
    loadListItems: LoadListItemsUseCase,
    initialFormValues: InitialFormValuesUseCase,
    submitForm: SubmitFormUseCase,
    private val createCalendarEvent: CreateCalendarEventUseCase,
    findMissingApiKeys: FindMissingApiKeysUseCase,
    loadScreen: LoadScreenUseCase,
) : AndroidViewModel(application) {

    /** One-off things only the activity can do. */
    sealed interface Effect {
        class OpenScreen(val view: MView, val payload: JSONObject?) : Effect
        class AddToCalendar(val entry: CalendarEntry) : Effect
    }

    private val effectChannel = Channel<Effect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    private val loaded = loadScreen(saved[EXTRA_VIEW_ID], saved[EXTRA_SCREEN_PAYLOAD])
        .also { if (it is LoadScreenUseCase.Result.Failed) alerts.show(it.message) }

    /** The parsed app, or null when it can't be shown (an alert says why). */
    val app: ModelApplication? = (loaded as? LoadScreenUseCase.Result.Ready)?.app

    /** What this screen shows, or null when it can't be shown (an alert says why). */
    val screen: ScreenRuntimeState? = (loaded as? LoadScreenUseCase.Result.Ready)?.screen

    val forms = FormsState(saved, initialFormValues, submitForm)
    val lists = ListsState(viewModelScope, loadListItems)

    /** The app's API keys this phone lacks: asked for before the screen shows, unless skipped. */
    var missingKeys by mutableStateOf(screen?.let { findMissingApiKeys() })
        private set

    /** The keys stay missing: the app opens without them, and what needs them can't load. */
    fun skipKeys() {
        missingKeys = null
    }

    /** True while a request started by an event is running. */
    var loading by mutableStateOf(false)
        private set
    private var requests = 0

    /** [view] as this app defines it, following a reference to a reusable view. */
    fun resolve(view: MView): MView = app?.viewBy(view) ?: view

    /** [action] as this app defines it, following a reference to a reusable action. */
    fun resolve(action: Action?): Action? = action?.let { app?.actionBy(it) }

    /** Runs [action]'s event with the action's own data, or [data] when it has none. */
    fun onAction(action: Action?, data: JSONObject?) {
        val resolved = resolve(action) ?: return
        resolved.event?.let { onEvent(it, resolved.dataObj ?: data) }
    }

    /** Sends [form] to its action's event, once every required field is filled in. */
    fun onSubmit(form: FormView, data: JSONObject?) {
        val json = forms.submit(form, data) ?: return
        resolve(form.action)?.event?.let { onEvent(it, json) }
    }

    fun onEvent(event: Event, data: JSONObject?) {
        when (event) {
            is AlertMessage -> showAlert(event, data)
            // The view receives the part of the data the mapping selects, such as "meals.0".
            is ShowView -> openScreen(event.view, event.mapping?.let { data?.getObject(it) } ?: data)
            is RESTService -> request(event, data)
            is CalendarEvent -> when (val result = createCalendarEvent(event, (event.mapping?.let { data?.getObject(it) } ?: data)?.toJsonObject())) {
                is CreateCalendarEventUseCase.Result.Ready -> effectChannel.trySend(Effect.AddToCalendar(result.entry))
                is CreateCalendarEventUseCase.Result.BadDate -> alerts.show(result.message)
            }
        }
    }

    private fun showAlert(event: AlertMessage, data: JSONObject?) {
        val message = (event.mapping?.let { data?.getValue(it) } ?: event.value)?.matchReplace(data)
        val ok = event.thenDoOK?.let { next -> Runnable { onEvent(next, data) } }
        // An alert that does something on OK can always be declined: Cancel then just closes it.
        val cancel = event.thenDoKO?.let { next -> Runnable { onEvent(next, data) } } ?: if (ok != null) Runnable { } else null
        alerts.show(message ?: "Missing message (check model)", ok, cancel)
    }

    private fun openScreen(view: MView, payload: JSONObject?) {
        val definition = app?.viewBy(view)
            ?: return alerts.show(getApplication<Application>().getString(R.string.view_missing, view.id.orEmpty()))
        effectChannel.trySend(Effect.OpenScreen(definition, payload ?: definition.dataObj))
    }

    private fun request(event: RESTService, data: JSONObject?) {
        viewModelScope.launch {
            requests += 1
            loading = true
            try {
                val response = fetchJson(event, data)
                event.thenDo?.let { onEvent(it, response) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                alerts.show(e.message ?: "An error occurred")
            } finally {
                requests -= 1
                loading = requests > 0
            }
        }
    }

    companion object {
        const val EXTRA_VIEW_ID = "idView"
        const val EXTRA_SCREEN_PAYLOAD = "screenPayload"
    }
}
