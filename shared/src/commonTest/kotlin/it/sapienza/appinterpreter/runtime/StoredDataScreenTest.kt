package it.sapienza.appinterpreter.runtime

import androidx.lifecycle.SavedStateHandle
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import it.sapienza.appinterpreter.Fixtures
import it.sapienza.appinterpreter.alerts.AlertsViewModel
import it.sapienza.appinterpreter.calendar.CreateCalendarEventUseCase
import it.sapienza.appinterpreter.json.JsonText
import it.sapienza.appinterpreter.json.getValue
import it.sapienza.appinterpreter.model.Layout
import it.sapienza.appinterpreter.model.view_model.FormView
import it.sapienza.appinterpreter.model.view_model.ListView
import it.sapienza.appinterpreter.runtime.usecase.FetchJsonUseCase
import it.sapienza.appinterpreter.runtime.usecase.FindMissingApiKeysUseCase
import it.sapienza.appinterpreter.runtime.usecase.InitialFormValuesUseCase
import it.sapienza.appinterpreter.runtime.usecase.LoadListItemsUseCase
import it.sapienza.appinterpreter.runtime.usecase.LoadScreenUseCase
import it.sapienza.appinterpreter.runtime.usecase.LoadStoredItemsUseCase
import it.sapienza.appinterpreter.runtime.usecase.OpenLinkUseCase
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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonObject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** The App ideas example driven without any UI: apps and their ideas saved on the phone, and the lists that show them. */
@OptIn(ExperimentalCoroutinesApi::class)
class StoredDataScreenTest {
    private val json = Fixtures.configurations.getValue("examples/v3_app_ideas.json")
    private val store = MemoryAppStore()
    private val alerts = AlertsViewModel()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun screen(appData: AppData, viewId: String? = null, payload: JsonObject? = null, app: String = json): ScreenViewModel {
        val current = CurrentConfiguration { app }
        val repository = AppRepository(current)
        val fetch = FetchJsonUseCase(JsonHttpClient(MockEngine { respond("{}") }, "Appkin/test"))
        val saved = SavedStateHandle(
            listOfNotNull(
                viewId?.let { ScreenViewModel.EXTRA_VIEW_ID to it },
                payload?.let { ScreenViewModel.EXTRA_SCREEN_PAYLOAD to it.toString() }
            ).toMap()
        )
        return ScreenViewModel(
            saved, alerts, fetch, LoadListItemsUseCase(fetch, LoadStoredItemsUseCase(appData, repository)), InitialFormValuesUseCase(),
            SubmitFormUseCase(), CreateCalendarEventUseCase(), SaveItemUseCase(appData, repository), OpenLinkUseCase(),
            FindMissingApiKeysUseCase(current, ConfigurationKeys(MemorySecretStore(), emptyMap()) { "x" }),
            LoadScreenUseCase(repository, EnglishRuntimeStrings), EnglishRuntimeStrings
        )
    }

    private val ScreenViewModel.views get() = (screen?.definition as Layout).views
    private fun ScreenViewModel.form() = assertIs<FormView>(views.first { it is FormView })
    private fun ScreenViewModel.list() = assertIs<ListView>(views.first { it is ListView })
    private fun ScreenViewModel.shown(data: JsonObject?, field: String) = lists.get(list(), data).content.value.items.map { it.getValue(field) }

    private fun ScreenViewModel.add(field: String, value: String, data: JsonObject? = null) {
        forms.set(form(), data, field, value)
        onSubmit(form(), data)
    }

    @Test
    fun anAppAddedWithTheFormAppearsInTheListAndTheFormEmpties() {
        val home = screen(AppData(store))
        assertEquals(emptyList(), home.shown(null, "name"))

        home.add("name", "Rainy")

        assertEquals(listOf("Rainy"), home.shown(null, "name"))
        assertNull(home.forms.values(home.form(), null)["name"])
    }

    @Test
    fun anIncompleteFormSavesNothing() {
        val home = screen(AppData(store))
        home.onSubmit(home.form(), null)
        assertEquals("Please complete: Name", home.forms.error(home.form(), null))
        assertEquals(emptyList(), home.shown(null, "name"))
    }

    @Test
    fun eachAppShowsOnlyItsOwnIdeasAndTheHomeListHearsOfChangesFromOtherScreens() = runTest {
        val appData = AppData(store)
        val home = screen(appData)
        home.add("name", "Rainy")
        home.add("name", "Tasse")
        val (rainy, tasse) = home.lists.get(home.list(), null).content.value.items

        // Tapping an app opens its screen with that app as data.
        home.onAction(home.list().action, rainy)
        val open = assertIs<ScreenViewModel.Effect.OpenScreen>(home.effects.first())
        assertEquals("app", open.view.id)
        val rainyScreen = screen(appData, "app", open.payload)
        val tasseScreen = screen(appData, "app", tasse)
        rainyScreen.add("title", "Dark mode", rainy)
        rainyScreen.add("title", "Widgets", rainy)
        tasseScreen.add("title", "Export to PDF", tasse)

        assertEquals(listOf("Dark mode", "Widgets"), rainyScreen.shown(rainy, "title"))
        assertEquals(listOf("Export to PDF"), tasseScreen.shown(tasse, "title"))
        assertEquals(listOf(rainy.getValue("id")), rainyScreen.shown(rainy, "appId").distinct())

        // A screen opened before a save shows it too.
        val before = screen(appData, "app", rainy)
        assertEquals(2, before.shown(rainy, "title").size)
        rainyScreen.add("title", "Sync", rainy)
        assertEquals(listOf("Dark mode", "Widgets", "Sync"), before.shown(rainy, "title"))
    }

    @Test
    fun savedDataIsThereInTheNextSessionAndBelongsToItsApp() {
        screen(AppData(store)).add("name", "Rainy")

        assertEquals(listOf("Rainy"), screen(AppData(store)).shown(null, "name"))
        val other = json.replace("\"App ideas\"", "\"Other ideas\"")
        assertEquals(emptyList(), screen(AppData(store), app = other).shown(null, "name"))
    }

    @Test
    fun thenDoGetsTheSavedObject() {
        val app = """
            {
              "schemaVersion": 3, "name": "Notes", "version": "1.0",
              "mainView": { "type": "button", "title": "Save", "data": { "text": "Hi" }, "action": { "id": "save", "event": {
                "type": "save", "collection": "notes", "fields": { "from": "${'$'}{who}" },
                "thenDo": { "type": "alert", "value": "Saved ${'$'}{text} from ${'$'}{from} as ${'$'}{id}" } } } }
            }
        """
        var ids = 0
        val screen = screen(AppData(store, newId = { "n${++ids}" }), app = app)

        screen.onAction(screen.screen?.definition?.action, JsonText.parse("""{ "text": "Hi", "who": "Ana" }""") as JsonObject)

        assertEquals("Saved Hi from Ana as n1", alerts.alert?.text)
    }
}
