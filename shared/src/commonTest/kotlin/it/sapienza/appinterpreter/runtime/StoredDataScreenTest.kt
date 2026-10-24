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
import it.sapienza.appinterpreter.model.event.AlertMessage
import it.sapienza.appinterpreter.model.event.DeleteData
import it.sapienza.appinterpreter.model.event.UpdateData
import it.sapienza.appinterpreter.model.view_model.ButtonIcon
import it.sapienza.appinterpreter.model.view_model.ButtonView
import it.sapienza.appinterpreter.model.view_model.CheckView
import it.sapienza.appinterpreter.model.view_model.FormView
import it.sapienza.appinterpreter.model.view_model.ListView
import it.sapienza.appinterpreter.runtime.usecase.DeleteItemsUseCase
import it.sapienza.appinterpreter.runtime.usecase.FetchJsonUseCase
import it.sapienza.appinterpreter.runtime.usecase.FindMissingApiKeysUseCase
import it.sapienza.appinterpreter.runtime.usecase.InitialFormValuesUseCase
import it.sapienza.appinterpreter.runtime.usecase.LoadListItemsUseCase
import it.sapienza.appinterpreter.runtime.usecase.LoadScreenUseCase
import it.sapienza.appinterpreter.runtime.usecase.LoadStoredItemsUseCase
import it.sapienza.appinterpreter.runtime.usecase.OpenLinkUseCase
import it.sapienza.appinterpreter.runtime.usecase.SaveItemUseCase
import it.sapienza.appinterpreter.runtime.usecase.SubmitFormUseCase
import it.sapienza.appinterpreter.runtime.usecase.UpdateItemUseCase
import it.sapienza.appinterpreter.security.ConfigurationKeys
import it.sapienza.appinterpreter.security.MemorySecretStore
import it.sapienza.appinterpreter.storage.AppData
import it.sapienza.appinterpreter.storage.MemoryAppStore
import it.sapienza.appinterpreter.utils.AppLibrary
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
    private val json = Fixtures.configurations.getValue("raw/v3_app_ideas.json")
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
            SubmitFormUseCase(), CreateCalendarEventUseCase(), SaveItemUseCase(appData, repository),
            UpdateItemUseCase(appData, repository), DeleteItemsUseCase(appData, repository), OpenLinkUseCase(),
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

    /** The example with Rainy and Tasse saved, each with ideas; the home screen and the two app screens. */
    private class Ideas(val home: ScreenViewModel, val rainy: JsonObject, val tasse: JsonObject, val rainyScreen: ScreenViewModel)

    private fun ideas(appData: AppData = AppData(store)): Ideas {
        val home = screen(appData)
        home.add("name", "Rainy")
        home.add("name", "Tasse")
        val (rainy, tasse) = home.lists.get(home.list(), null).content.value.items
        val rainyScreen = screen(appData, "app", rainy)
        rainyScreen.add("title", "Dark mode", rainy)
        rainyScreen.forms.set(rainyScreen.form(), rainy, "priority", "Low")
        rainyScreen.add("title", "Widgets", rainy)
        screen(appData, "app", tasse).add("title", "Export to PDF", tasse)
        return Ideas(home, rainy, tasse, rainyScreen)
    }

    /** The row view of [list]'s items: a horizontal layout of (in this example) the item's views and its buttons. */
    private fun ScreenViewModel.row() = assertIs<Layout>(list().itemView).views

    private fun ScreenViewModel.items(data: JsonObject?) = lists.get(list(), data).content.value.items

    @Test
    fun aNewIdeaIsNotDoneAndOnlyADoneIdeaShowsDone() {
        val ideas = ideas()
        val (rainy, screen) = ideas.rainy to ideas.rainyScreen
        assertEquals(listOf("false", "false"), screen.shown(rainy, "done"))
        val doneLine = assertIs<Layout>(screen.row()[0]).views.last()
        assertEquals("done", doneLine.visibleIf)

        screen.onAction(screen.list().swipeToEnd, screen.items(rainy)[0])

        assertEquals(listOf(false, true), screen.items(rainy).map { ViewVisibility.holds(doneLine.visibleIf, it) })
    }

    @Test
    fun aCheckViewSavesItsValueAtOnce() {
        val app = """
            {
              "schemaVersion": 3, "name": "Tasks", "version": "1.0",
              "mainView": { "type": "list", "stored": { "collection": "tasks" },
                "itemView": { "type": "check", "mapping": "done", "title": "${'$'}{title}",
                  "action": { "id": "tick", "event": { "type": "update", "collection": "tasks" } } } }
            }
        """
        val appData = AppData(store, newId = { "t1" })
        appData.add(AppLibrary.idOf("Tasks"), "tasks", JsonText.parse("""{ "title": "Buy milk", "done": false }""") as JsonObject)
        val screen = screen(appData, app = app)
        val list = assertIs<ListView>(screen.screen?.definition)
        val check = assertIs<CheckView>(list.itemView)
        fun done() = screen.lists.get(list, null).content.value.items.single().getValue("done")

        screen.onCheck(check, screen.lists.get(list, null).content.value.items.single(), checked = true)
        assertEquals("true", done())
        screen.onCheck(check, screen.lists.get(list, null).content.value.items.single(), checked = false)
        assertEquals("false", done())
    }

    @Test
    fun ideasHaveADescriptionAndAPriorityAndAreSortedByIt() {
        val ideas = ideas()
        val screen = ideas.rainyScreen
        // Medium by default; Low was picked for Widgets.
        assertEquals(listOf("Dark mode", "Widgets"), screen.shown(ideas.rainy, "title"))

        screen.forms.set(screen.form(), ideas.rainy, "description", "A tile for today's rain")
        screen.forms.set(screen.form(), ideas.rainy, "priority", "High")
        screen.add("title", "Tile", ideas.rainy)
        screen.add("title", "Sync", ideas.rainy)

        assertEquals(listOf("Tile", "Dark mode", "Sync", "Widgets"), screen.shown(ideas.rainy, "title"))
        assertEquals(listOf("High", "Medium", "Medium", "Low"), screen.shown(ideas.rainy, "priority"))
        assertEquals("A tile for today's rain", screen.items(ideas.rainy)[0].getValue("description"))
        assertEquals("Medium", screen.forms.values(screen.form(), ideas.rainy)["priority"], "the form starts again at Medium")
    }

    @Test
    fun swipingAnIdeaAwayAsksBeforeDeleting() {
        val ideas = ideas()
        val screen = ideas.rainyScreen
        val swipe = screen.list().swipeToStart

        screen.onAction(swipe, screen.items(ideas.rainy)[0])
        assertEquals("Delete Dark mode?", alerts.alert?.text)
        alerts.answer(ok = false)
        assertEquals(listOf("Dark mode", "Widgets"), screen.shown(ideas.rainy, "title"))

        screen.onAction(swipe, screen.items(ideas.rainy)[0])
        alerts.answer(ok = true)
        assertEquals(listOf("Widgets"), screen.shown(ideas.rainy, "title"))
    }

    @Test
    fun swipingAnAppAwayDeletesItsIdeasButNoOthers() {
        val ideas = ideas()
        ideas.home.onAction(ideas.home.list().swipeToStart, ideas.rainy)
        assertEquals("Delete Rainy and its ideas?", alerts.alert?.text)
        alerts.answer(ok = true)

        assertEquals(listOf("Tasse"), ideas.home.shown(null, "name"))
        val saved = AppData(store).items(AppLibrary.idOf("App ideas"), "ideas")
        assertEquals(listOf("Export to PDF"), saved.map { it.getValue("title") })
        assertEquals(emptyList(), ideas.rainyScreen.shown(ideas.rainy, "title"), "a screen further back updates too")
    }

    @Test
    fun aFormThatUpdatesEditsTheItemItShowsAndKeepsItsValues() {
        val app = """
            {
              "schemaVersion": 3, "name": "Notes", "version": "1.0",
              "mainView": { "type": "form", "formElements": [ { "type": "text", "mapping": "text", "title": "Text" } ],
                "action": { "id": "edit", "event": { "type": "update", "collection": "notes", "fields": { "edited": true },
                  "thenDo": { "type": "alert", "value": "Now ${'$'}{text}" } } } }
            }
        """
        val appData = AppData(store, newId = { "n1" })
        val saved = appData.add(AppLibrary.idOf("Notes"), "notes", JsonText.parse("""{ "text": "Hi" }""") as JsonObject)
        val screen = screen(appData, app = app)
        val form = assertIs<FormView>(screen.screen?.definition)

        screen.forms.set(form, saved, "text", "Hello")
        screen.onSubmit(form, saved)

        assertEquals("Now Hello", alerts.alert?.text)
        val note = appData.items(AppLibrary.idOf("Notes"), "notes").single()
        assertEquals(listOf("n1", "Hello", "true"), listOf(note.getValue("id"), note.getValue("text"), note.getValue("edited")))
        assertEquals("Hello", screen.forms.values(form, saved)["text"])
    }

    @Test
    fun changingAnItemThatIsGoneSaysSo() {
        val home = screen(AppData(store))
        home.onEvent(UpdateData("apps", fields = null), JsonText.parse("""{ "id": "gone", "name": "Old" }""") as JsonObject)
        assertEquals("Nothing to change: this item is no longer saved", alerts.alert?.text)
    }

    @Test
    fun deletingWhatIsNotSavedIsNoMistake() {
        val home = screen(AppData(store))
        home.onEvent(DeleteData("apps", thenDo = AlertMessage(value = "Done")), JsonText.parse("""{ "id": "gone" }""") as JsonObject)
        assertEquals("Done", alerts.alert?.text)
    }

    @Test
    fun tappingAnIdeaEditsItAndSavingGoesBack() = runTest {
        val ideas = ideas()
        val list = ideas.rainyScreen.list()
        val darkMode = ideas.rainyScreen.items(ideas.rainy).first { it.getValue("title") == "Dark mode" }

        ideas.rainyScreen.onAction(list.action, darkMode)
        val open = assertIs<ScreenViewModel.Effect.OpenScreen>(ideas.rainyScreen.effects.first())
        assertEquals("idea", open.view.id)
        assertEquals("Edit idea", open.title)
        val edit = screen(AppData(store), "idea", open.payload)
        val form = edit.form()
        assertEquals("Dark mode", edit.forms.values(form, open.payload)["title"], "the form starts filled in")
        assertEquals("Medium", edit.forms.values(form, open.payload)["priority"])

        edit.forms.set(form, open.payload, "title", "Dark theme")
        edit.forms.set(form, open.payload, "priority", "High")
        edit.onSubmit(form, open.payload)

        assertIs<ScreenViewModel.Effect.Close>(edit.effects.first())
        val saved = AppData(store).items(AppLibrary.idOf("App ideas"), "ideas").first { it.getValue("id") == darkMode.getValue("id") }
        assertEquals(listOf("Dark theme", "High", ideas.rainy.getValue("id")), listOf("title", "priority", "appId").map { saved.getValue(it) })
    }

    @Test
    fun doneIdeasGoLastThenByPriority() {
        val ideas = ideas()
        val screen = ideas.rainyScreen
        screen.forms.set(screen.form(), ideas.rainy, "priority", "High")
        screen.add("title", "Tile", ideas.rainy)
        assertEquals(listOf("Tile", "Dark mode", "Widgets"), screen.shown(ideas.rainy, "title"))

        screen.onAction(screen.list().swipeToEnd, screen.items(ideas.rainy)[0])

        assertEquals(listOf("Dark mode", "Widgets", "Tile"), screen.shown(ideas.rainy, "title"))
    }

    @Test
    fun theCopyButtonCopiesTheIdeaWithoutLinesItHasNoValueFor() = runTest {
        val ideas = ideas()
        val screen = ideas.rainyScreen
        val copy = screen.row().filterIsInstance<ButtonView>().first { it.icon == ButtonIcon.copy }
        screen.forms.set(screen.form(), ideas.rainy, "description", "A tile for today's rain")
        screen.add("title", "Tile", ideas.rainy)
        val tile = screen.items(ideas.rainy).first { it.getValue("title") == "Tile" }
        val darkMode = screen.items(ideas.rainy).first { it.getValue("title") == "Dark mode" }

        screen.onAction(copy.action, tile)
        assertEquals(
            "Tile\nA tile for today's rain\nPriority: Medium\nAn idea for my app Rainy",
            assertIs<ScreenViewModel.Effect.Copy>(screen.effects.first()).text
        )
        screen.onAction(copy.action, darkMode)
        assertEquals("Dark mode\nPriority: Medium\nAn idea for my app Rainy", assertIs<ScreenViewModel.Effect.Copy>(screen.effects.first()).text)
    }

    @Test
    fun swipingAnIdeaTowardsTheEndMarksItDone() {
        val ideas = ideas()
        val screen = ideas.rainyScreen
        val widgets = screen.items(ideas.rainy).first { it.getValue("title") == "Widgets" }

        screen.onAction(screen.list().swipeToEnd, widgets)

        assertEquals(listOf("Dark mode", "Widgets"), screen.shown(ideas.rainy, "title"), "done ideas go last")
        assertEquals("true", screen.items(ideas.rainy).last().getValue("done"))
    }

    @Test
    fun aUniqueSaveReplacesTheObjectWithTheSameValueInsteadOfAddingAnother() {
        val app = """
            {
              "schemaVersion": 3, "name": "Gallery", "version": "1.0",
              "mainView": { "type": "button", "title": "Favourite", "action": { "id": "fav", "event": {
                "type": "save", "collection": "favourites", "unique": "key" } } }
            }
        """
        var ids = 0
        val appData = AppData(store, newId = { "f${++ids}" })
        val screen = screen(appData, app = app)
        val action = screen.screen?.definition?.action
        fun work(key: String, title: String) = JsonText.parse("""{ "key": "$key", "title": "$title" }""") as JsonObject

        screen.onAction(action, work("mona-lisa", "Mona Lisa"))
        screen.onAction(action, work("mona-lisa", "Mona Lisa (again)"))
        screen.onAction(action, work("the-kiss", "The Kiss"))

        val saved = appData.items(AppLibrary.idOf("Gallery"), "favourites")
        assertEquals(listOf("f1", "f2"), saved.map { it.getValue("id") }, "the first one kept its id")
        assertEquals(listOf("Mona Lisa (again)", "The Kiss"), saved.map { it.getValue("title") })
    }
}
