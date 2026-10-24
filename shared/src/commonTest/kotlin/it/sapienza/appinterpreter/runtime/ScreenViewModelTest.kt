package it.sapienza.appinterpreter.runtime

import androidx.lifecycle.SavedStateHandle
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import it.sapienza.appinterpreter.alerts.AlertsViewModel
import it.sapienza.appinterpreter.calendar.CreateCalendarEventUseCase
import it.sapienza.appinterpreter.json.getValue
import it.sapienza.appinterpreter.model.Layout
import it.sapienza.appinterpreter.model.view_model.ButtonView
import it.sapienza.appinterpreter.model.view_model.FormView
import it.sapienza.appinterpreter.runtime.usecase.FetchJsonUseCase
import it.sapienza.appinterpreter.runtime.usecase.FindMissingApiKeysUseCase
import it.sapienza.appinterpreter.runtime.usecase.InitialFormValuesUseCase
import it.sapienza.appinterpreter.runtime.usecase.LoadListItemsUseCase
import it.sapienza.appinterpreter.runtime.usecase.LoadScreenUseCase
import it.sapienza.appinterpreter.runtime.usecase.LoadStoredItemsUseCase
import it.sapienza.appinterpreter.runtime.usecase.SaveItemUseCase
import it.sapienza.appinterpreter.runtime.usecase.SubmitFormUseCase
import it.sapienza.appinterpreter.security.ConfigurationKeys
import it.sapienza.appinterpreter.security.MemorySecretStore
import it.sapienza.appinterpreter.storage.AppData
import it.sapienza.appinterpreter.storage.MemoryAppStore
import it.sapienza.appinterpreter.utils.httpclient.JsonHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** A screen of a JSON app driven without any UI, as an iPhone app's SwiftUI view would drive it. */
@OptIn(ExperimentalCoroutinesApi::class)
class ScreenViewModelTest {
    private val json = """
        {
          "schemaVersion": 3, "name": "Screen test", "version": "1.0",
          "mainView": { "type": "layout", "data": { "who": "Ana" }, "views": [
            { "type": "button", "title": "Hello", "action": { "id": "hello", "event": { "type": "alert", "value": "Hi ${'$'}{who}",
              "thenDoOK": { "type": "show", "view": { "ref": "details" } } } } },
            { "type": "button", "title": "Load", "action": { "id": "load", "event": { "type": "rest", "url": "https://example.com/__KEY__",
              "thenDo": { "type": "alert", "mapping": "message" } } } },
            { "type": "form", "formElements": [ { "type": "text", "mapping": "name", "title": "Name", "mandatory": true } ],
              "action": { "id": "send", "event": { "type": "calendar", "title": "${'$'}{name}", "date": "01/05/2026" } } }
          ] },
          "views": [ { "id": "details", "type": "text", "title": "Details" } ]
        }
    """
    private val alerts = AlertsViewModel()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun screen(): ScreenViewModel {
        val current = CurrentConfiguration { json }
        val keys = ConfigurationKeys(MemorySecretStore(), mapOf("test" to mapOf("KEY" to "k"))) { "bundled:test" }
        val fetch = FetchJsonUseCase(
            JsonHttpClient(MockEngine { respond("""{ "message": "Loaded from ${it.url.encodedPath}" }""") }, "Appkin/test", keys::replaceForCurrentConfiguration)
        )
        val repository = AppRepository(current)
        val appData = AppData(MemoryAppStore())
        return ScreenViewModel(
            SavedStateHandle(), alerts, fetch, LoadListItemsUseCase(fetch, LoadStoredItemsUseCase(appData, repository)), InitialFormValuesUseCase(),
            SubmitFormUseCase(), CreateCalendarEventUseCase(), SaveItemUseCase(appData, repository), FindMissingApiKeysUseCase(current, keys),
            LoadScreenUseCase(repository, EnglishRuntimeStrings), EnglishRuntimeStrings
        )
    }

    @Test
    fun anAlertThenAScreenWithTheData() = runTest {
        val screen = screen()
        val layout = assertIs<Layout>(screen.screen?.definition)
        assertNull(screen.missingKeys.value, "the bundled key is there")
        val hello = assertIs<ButtonView>(layout.views[0])

        screen.onAction(hello.action, screen.screen?.data)
        assertEquals("Hi Ana", alerts.alert?.text)
        alerts.answer(ok = true)

        val open = assertIs<ScreenViewModel.Effect.OpenScreen>(screen.effects.first())
        assertEquals("details", open.view.id)
        assertEquals("Ana", open.payload?.getValue("who"))
    }

    @Test
    fun aRequestShowsItsAnswerAndLoadingEnds() = runTest {
        val screen = screen()
        val load = assertIs<ButtonView>((screen.screen?.definition as Layout).views[1])

        screen.onAction(load.action, null)

        // The engine answers on its own thread: wait for the alert.
        val alert = withContext(Dispatchers.Default) { withTimeout(10_000) { alerts.alertState.first { it != null } } }
        assertEquals("Loaded from /k", alert?.text)
        withContext(Dispatchers.Default) { withTimeout(10_000) { screen.loading.first { !it } } }
        assertFalse(screen.loading.value)
    }

    @Test
    fun aFormIsSentOnlyWhenCompleteAndThenAddsACalendarEvent() = runTest {
        val screen = screen()
        val form = assertIs<FormView>((screen.screen?.definition as Layout).views[2])

        screen.onSubmit(form, null)
        assertEquals("Please complete: Name", screen.forms.state(form, null).error.value)

        screen.forms.set(form, null, "name", "Race day")
        screen.onSubmit(form, null)
        val calendar = assertIs<ScreenViewModel.Effect.AddToCalendar>(screen.effects.first())
        assertEquals("Race day", calendar.entry.title)
        assertNotNull(calendar.entry.beginMillis)
    }

    @Test
    fun anUnknownViewIsReported() {
        val repository = AppRepository({ json })
        val appData = AppData(MemoryAppStore())
        val failed = ScreenViewModel(
            SavedStateHandle(mapOf(ScreenViewModel.EXTRA_VIEW_ID to "nowhere")), alerts,
            FetchJsonUseCase(JsonHttpClient(MockEngine { respond("{}") }, "Appkin/test")),
            LoadListItemsUseCase(
                FetchJsonUseCase(JsonHttpClient(MockEngine { respond("{}") }, "Appkin/test")),
                LoadStoredItemsUseCase(appData, repository)
            ),
            InitialFormValuesUseCase(), SubmitFormUseCase(), CreateCalendarEventUseCase(), SaveItemUseCase(appData, repository),
            FindMissingApiKeysUseCase({ json }, ConfigurationKeys(MemorySecretStore(), emptyMap()) { "x" }),
            LoadScreenUseCase(repository, EnglishRuntimeStrings), EnglishRuntimeStrings
        )
        assertNull(failed.screen)
        assertEquals("View 'nowhere' does not exist in this configuration", alerts.alert?.text)
    }
}
