package it.sapienza.appinterpreter.runtime

import androidx.lifecycle.SavedStateHandle
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import it.sapienza.appinterpreter.model.view_model.form_model.TagsInput
import it.sapienza.appinterpreter.model.view_model.TextView
import it.sapienza.appinterpreter.json.matchReplace
import it.sapienza.appinterpreter.json.PlaceholderClock
import it.sapienza.appinterpreter.model.view_model.form_model.PhotosInput
import it.sapienza.appinterpreter.Fixtures
import it.sapienza.appinterpreter.alerts.AlertsViewModel
import it.sapienza.appinterpreter.calendar.CreateCalendarEventUseCase
import it.sapienza.appinterpreter.json.JsonText
import it.sapienza.appinterpreter.json.getValue
import it.sapienza.appinterpreter.model.Layout
import it.sapienza.appinterpreter.model.event.AlertMessage
import it.sapienza.appinterpreter.model.event.DeleteData
import it.sapienza.appinterpreter.model.event.SaveData
import it.sapienza.appinterpreter.model.event.UpdateData
import it.sapienza.appinterpreter.model.view_model.ButtonIcon
import it.sapienza.appinterpreter.model.view_model.ButtonView
import it.sapienza.appinterpreter.model.view_model.CheckView
import it.sapienza.appinterpreter.model.view_model.FormView
import it.sapienza.appinterpreter.model.view_model.ListView
import it.sapienza.appinterpreter.model.view_model.helper.MView
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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlin.test.assertTrue
import kotlin.test.assertFalse
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
    fun anAppAddedOnItsOwnScreenAppearsInTheListWithItsDescription() = runTest {
        val appData = AppData(store)
        val home = screen(appData)
        assertEquals(emptyList(), home.shown(null, "name"))

        home.onAction(assertIs<ButtonView>(home.views.first { it is ButtonView }).action, null)
        val open = assertIs<ScreenViewModel.Effect.OpenScreen>(home.effects.first())
        assertEquals(listOf("new_app", "New app"), listOf(open.view.id, open.title))
        val newApp = screen(appData, "new_app")
        newApp.forms.set(newApp.form(), null, "description", "Rain alerts\non the wrist")
        newApp.add("name", "Rainy")

        assertIs<ScreenViewModel.Effect.Close>(newApp.effects.first(), "saving goes back to the list")
        assertEquals(listOf("Rainy"), home.shown(null, "name"))
        assertEquals(listOf("Rain alerts\non the wrist"), home.shown(null, "description"))
        assertNull(newApp.forms.values(newApp.form(), null)["name"], "the form starts empty again")
    }

    @Test
    fun anIncompleteFormSavesNothing() {
        val appData = AppData(store)
        val newApp = screen(appData, "new_app")
        newApp.onSubmit(newApp.form(), null)
        assertEquals("Please complete: Name", newApp.forms.error(newApp.form(), null))
        assertEquals(emptyList(), screen(appData).shown(null, "name"))
    }

    /** Adds the apps [names] on the new-app screen. */
    private fun addApps(appData: AppData, vararg names: String) {
        val newApp = screen(appData, "new_app")
        names.forEach { newApp.add("name", it) }
    }

    @Test
    fun eachAppShowsOnlyItsOwnIdeasAndTheHomeListHearsOfChangesFromOtherScreens() = runTest {
        val appData = AppData(store)
        val home = screen(appData)
        addApps(appData, "Rainy", "Tasse")
        val (rainy, tasse) = home.lists.get(home.list(), null).content.value.items

        // Tapping an app opens its screen with that app as data.
        home.onAction(home.list().action, rainy)
        val open = assertIs<ScreenViewModel.Effect.OpenScreen>(home.effects.first())
        assertEquals("app", open.view.id)
        val rainyScreen = screen(appData, "app", open.payload)
        val tasseScreen = screen(appData, "app", tasse)
        // New ideas are written on their own screen, opened from the app's with the app as data.
        rainyScreen.onAction(rainyScreen.newIdeaButton().action, rainy)
        val newIdea = assertIs<ScreenViewModel.Effect.OpenScreen>(rainyScreen.effects.first())
        assertEquals(listOf("new_idea", "New idea for Rainy"), listOf(newIdea.view.id, newIdea.title))
        val rainyNew = screen(appData, "new_idea", newIdea.payload)
        rainyNew.add("title", "Dark mode", rainy)
        assertIs<ScreenViewModel.Effect.Close>(rainyNew.effects.first(), "saving goes back to the list")
        rainyNew.add("title", "Widgets", rainy)
        screen(appData, "new_idea", tasse).add("title", "Export to PDF", tasse)

        assertEquals(listOf("Dark mode", "Widgets"), rainyScreen.shown(rainy, "title"))
        assertEquals(listOf("Export to PDF"), tasseScreen.shown(tasse, "title"))
        assertEquals(listOf(rainy.getValue("id")), rainyScreen.shown(rainy, "appId").distinct())

        // A screen opened before a save shows it too.
        val before = screen(appData, "app", rainy)
        assertEquals(2, before.shown(rainy, "title").size)
        rainyNew.add("title", "Sync", rainy)
        assertEquals(listOf("Dark mode", "Widgets", "Sync"), before.shown(rainy, "title"))
    }

    @Test
    fun savedDataIsThereInTheNextSessionAndBelongsToItsApp() {
        addApps(AppData(store), "Rainy")

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
    private class Ideas(
        val home: ScreenViewModel,
        val rainy: JsonObject,
        val tasse: JsonObject,
        val rainyScreen: ScreenViewModel,
        /** The new-idea screen for Rainy. */
        val rainyNew: ScreenViewModel
    )

    private fun ideas(appData: AppData = AppData(store)): Ideas {
        val home = screen(appData)
        addApps(appData, "Rainy", "Tasse")
        val (rainy, tasse) = home.lists.get(home.list(), null).content.value.items
        val rainyScreen = screen(appData, "app", rainy)
        val rainyNew = screen(appData, "new_idea", rainy)
        rainyNew.add("title", "Dark mode", rainy)
        rainyNew.forms.set(rainyNew.form(), rainy, "priority", "Low")
        rainyNew.add("title", "Widgets", rainy)
        screen(appData, "new_idea", tasse).add("title", "Export to PDF", tasse)
        return Ideas(home, rainy, tasse, rainyScreen, rainyNew)
    }

    /** The app screen's button that opens the new-idea screen. */
    private fun ScreenViewModel.newIdeaButton() = assertIs<ButtonView>(views.first { it is ButtonView && it.title == "New idea" })

    /** The views of [list]'s items: the item view's own (the title row first), in order. */
    private fun ScreenViewModel.row() = assertIs<Layout>(list().itemView).views

    /** Every view inside [list]'s item view, nested ones included. */
    private fun ScreenViewModel.itemViews(): List<MView> {
        fun all(view: MView): List<MView> = listOf(view) + ((view as? Layout)?.views.orEmpty().flatMap(::all))
        return all(list().itemView)
    }

    private fun ScreenViewModel.items(data: JsonObject?) = lists.get(list(), data).content.value.items

    @Test
    fun aNewIdeaIsNotDoneAndSwipingRightMarksItDone() {
        val ideas = ideas()
        val (rainy, screen) = ideas.rainy to ideas.rainyScreen
        assertEquals(listOf("false", "false"), screen.shown(rainy, "done"))
        val doneLine = screen.row().first { it.visibleIf == "done" }

        screen.onAction(screen.list().swipeToEnd, screen.items(rainy)[0])

        assertEquals(listOf("false", "true"), screen.shown(rainy, "done"), "done ideas go last")
        assertEquals(listOf(false, true), screen.items(rainy).map { ViewVisibility.holds(doneLine.visibleIf, it) })
    }

    @Test
    fun aDetailScreensTickShowsAtOnceAndASecondTapTurnsItBack() {
        val app = """
            {
              "schemaVersion": 3, "name": "Trip", "version": "1.0",
              "mainView": { "type": "list", "stored": { "collection": "places" },
                "itemView": { "type": "text", "title": "${'$'}{name}" },
                "action": { "id": "open", "event": { "type": "show", "view": { "ref": "place" } } } },
              "views": [ { "id": "place", "type": "layout", "views": [
                { "type": "text", "title": "${'$'}{name}" },
                { "type": "check", "mapping": "visited", "title": "Visited",
                  "action": { "id": "tick", "event": { "type": "update", "collection": "places" } } } ] } ]
            }
        """
        val appData = AppData(store, newId = { "p1" })
        val place = appData.add(AppLibrary.idOf("Trip"), "places", JsonText.parse("""{ "name": "Bastion", "visited": false }""") as JsonObject)
        val detail = screen(appData, viewId = "place", payload = place, app = app)
        val check = (detail.screen?.definition as Layout).views.filterIsInstance<CheckView>().single()

        detail.onCheck(check, detail.data.value, checked = true)
        assertEquals("true", detail.data.value?.getValue("visited"), "the screen shows the tick")
        assertEquals("Bastion", detail.data.value?.getValue("name"), "and keeps the rest")
        detail.onCheck(check, detail.data.value, checked = false)
        assertEquals("false", detail.data.value?.getValue("visited"))
        assertEquals("false", appData.items(AppLibrary.idOf("Trip"), "places").single().getValue("visited"))
    }

    @Test
    fun anUpdateOfAnotherObjectLeavesTheScreensDataAlone() {
        val appData = AppData(store)
        val shown = JsonText.parse("""{ "id": "a", "name": "Rainy" }""") as JsonObject
        val other = appData.add(AppLibrary.idOf("App ideas"), "apps", JsonText.parse("""{ "name": "Tasse" }""") as JsonObject)
        val screen = screen(appData, viewId = "new_app", payload = shown)
        screen.onEvent(UpdateData("apps", fields = JsonObject(mapOf("name" to JsonPrimitive("Tasse 2")))), other)
        assertEquals("Rainy", screen.data.value?.getValue("name"))
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

    /** A trip checklist: three places it starts with, ticked off as visited, the visited ones last. */
    private val checklist = """
        {
          "schemaVersion": 3, "name": "Trip", "version": "1.0",
          "mainView": { "type": "list",
            "stored": { "collection": "places", "sort": [{ "by": "visited" }, { "by": "order" }],
              "initialItems": [
                { "name": "Parliament", "photo": "https://example.com/p.jpg", "visited": false, "order": 1 },
                { "name": "Bastion", "photo": "https://example.com/b.jpg", "visited": false, "order": 2 },
                { "name": "Baths", "photo": "https://example.com/t.jpg", "visited": false, "order": 3 }
              ] },
            "itemView": { "type": "check", "mapping": "visited", "title": "${'$'}{name}",
              "action": { "id": "tick", "event": { "type": "update", "collection": "places" } } } }
        }
    """

    @Test
    fun aStoredListStartsWithItsInitialItemsWithEveryField() {
        val screen = screen(AppData(store), app = checklist)
        val list = assertIs<ListView>(screen.screen?.definition)
        val items = screen.lists.get(list, null).content.value.items
        assertEquals(listOf("Parliament", "Bastion", "Baths"), items.map { it.getValue("name") })
        assertEquals("https://example.com/b.jpg", items[1].getValue("photo"))
        assertEquals(3, items.map { it.getValue("id") }.toSet().size, "each one gets its own id")
    }

    @Test
    fun aTickedInitialItemGoesLastAndStaysTickedWhenTheAppOpensAgain() {
        val appData = AppData(store)
        val screen = screen(appData, app = checklist)
        val list = assertIs<ListView>(screen.screen?.definition)
        fun names(on: ScreenViewModel) = on.lists.get(list, null).content.value.items.map { it.getValue("name") }

        screen.onCheck(assertIs<CheckView>(list.itemView), screen.lists.get(list, null).content.value.items.first(), checked = true)
        assertEquals(listOf("Bastion", "Baths", "Parliament"), names(screen), "visited (true) after not visited (false)")

        val again = screen(AppData(store), app = checklist)
        assertEquals(listOf("Bastion", "Baths", "Parliament"), names(again), "the initial items aren't saved again")
        assertEquals(3, appData.items(AppLibrary.idOf("Trip"), "places").size)
    }

    @Test
    fun initialItemsDontComeBackOnceTheUserDeletedThem() {
        val appData = AppData(store)
        val first = screen(appData, app = checklist)
        assertEquals(3, first.lists.get(assertIs<ListView>(first.screen?.definition), null).content.value.items.size)
        appData.delete(AppLibrary.idOf("Trip"), "places") { true }
        val again = screen(AppData(store), app = checklist)
        assertEquals(emptyList(), again.lists.get(assertIs<ListView>(again.screen?.definition), null).content.value.items)
    }

    @Test
    fun aNewVersionsInitialItemsJoinTheListWithoutUndoingTicks() {
        fun version(vararg places: String) = """
            {
              "schemaVersion": 3, "name": "Trip", "version": "1.0",
              "mainView": { "type": "list",
                "stored": { "collection": "places", "unique": "key",
                  "initialItems": [${places.joinToString { """{ "key": "$it", "name": "$it", "visited": false }""" }}] },
                "itemView": { "type": "check", "mapping": "visited", "title": "${'$'}{name}",
                  "action": { "id": "tick", "event": { "type": "update", "collection": "places" } } } }
            }
        """
        val first = screen(AppData(store), app = version("Parliament", "Bastion"))
        val list = assertIs<ListView>(first.screen?.definition)
        first.onCheck(assertIs<CheckView>(list.itemView), first.lists.get(list, null).content.value.items.first(), checked = true)

        val second = screen(AppData(store), app = version("Parliament", "Bastion", "Baths"))
        val items = second.lists.get(assertIs<ListView>(second.screen?.definition), null).content.value.items
        assertEquals(listOf("Parliament", "Bastion", "Baths"), items.map { it.getValue("name") })
        assertEquals(listOf("true", "false", "false"), items.map { it.getValue("visited") })
    }

    @Test
    fun anEventsOwnDataIsWhatItSaves() {
        val appData = AppData(store)
        val home = screen(appData)
        val save = SaveData("apps", thenDo = SaveData("apps", data = JsonText.parse("""{ "name": "Tasse" }""") as JsonObject),
            data = JsonText.parse("""{ "name": "Rainy" }""") as JsonObject)
        home.onEvent(save, JsonText.parse("""{ "name": "ignored" }""") as JsonObject)
        assertEquals(listOf("Rainy", "Tasse"), appData.items(AppLibrary.idOf("App ideas"), "apps").map { it.getValue("name") })
    }

    @Test
    fun aFormStillSavesWhatWasTypedWhenItsEventHasData() {
        val app = """
            {
              "schemaVersion": 3, "name": "Notes", "version": "1.0",
              "mainView": { "type": "form", "formElements": [{ "type": "text", "mapping": "text", "title": "Text" }],
                "action": { "id": "keep", "event": { "type": "save", "collection": "notes", "data": { "text": "fixed" } } } }
            }
        """
        val appData = AppData(store)
        val screen = screen(appData, app = app)
        val form = assertIs<FormView>(screen.screen?.definition)
        screen.forms.set(form, null, "text", "typed")
        screen.onSubmit(form, null)
        assertEquals(listOf("typed"), appData.items(AppLibrary.idOf("Notes"), "notes").map { it.getValue("text") })
    }

    @Test
    fun ideasHaveADescriptionAndAPriorityAndAreSortedByIt() {
        val ideas = ideas()
        val screen = ideas.rainyScreen
        // Medium by default; Low was picked for Widgets.
        assertEquals(listOf("Dark mode", "Widgets"), screen.shown(ideas.rainy, "title"))

        ideas.rainyNew.forms.set(ideas.rainyNew.form(), ideas.rainy, "description", "A tile for today's rain")
        ideas.rainyNew.forms.set(ideas.rainyNew.form(), ideas.rainy, "priority", "High")
        ideas.rainyNew.add("title", "Tile", ideas.rainy)
        ideas.rainyNew.add("title", "Sync", ideas.rainy)

        assertEquals(listOf("Tile", "Dark mode", "Sync", "Widgets"), screen.shown(ideas.rainy, "title"))
        assertEquals(listOf("High", "Medium", "Medium", "Low"), screen.shown(ideas.rainy, "priority"))
        assertEquals("A tile for today's rain", screen.items(ideas.rainy)[0].getValue("description"))
        assertEquals("Medium", ideas.rainyNew.forms.values(ideas.rainyNew.form(), ideas.rainy)["priority"], "the form starts again at Medium")
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
        ideas.rainyNew.forms.set(ideas.rainyNew.form(), ideas.rainy, "priority", "High")
        ideas.rainyNew.add("title", "Tile", ideas.rainy)
        assertEquals(listOf("Tile", "Dark mode", "Widgets"), screen.shown(ideas.rainy, "title"))

        screen.onAction(screen.list().swipeToEnd, screen.items(ideas.rainy)[0])

        assertEquals(listOf("Dark mode", "Widgets", "Tile"), screen.shown(ideas.rainy, "title"))
    }

    @Test
    fun theShareButtonSharesTheIdeaWithItsPhotos() = runTest {
        val ideas = ideas()
        val screen = ideas.rainyScreen
        val share = screen.itemViews().filterIsInstance<ButtonView>().single { it.icon == ButtonIcon.share }
        assertEquals(share, assertIs<Layout>(screen.row()[0]).views.last(), "the share button ends the title row")
        ideas.rainyNew.forms.set(ideas.rainyNew.form(), ideas.rainy, "description", "A tile for today's rain")
        ideas.rainyNew.forms.set(
            ideas.rainyNew.form(), ideas.rainy, "photos",
            JsonArray(listOf(JsonPrimitive("appkin-photo:a.jpg"), JsonPrimitive("appkin-photo:b.jpg")))
        )
        ideas.rainyNew.add("title", "Tile", ideas.rainy)
        val tile = screen.items(ideas.rainy).first { it.getValue("title") == "Tile" }
        val darkMode = screen.items(ideas.rainy).first { it.getValue("title") == "Dark mode" }

        screen.onAction(share.action, tile)
        val shared = assertIs<ScreenViewModel.Effect.Share>(screen.effects.first())
        assertEquals("Tile\nA tile for today's rain", shared.text, "title and description only")
        assertEquals(listOf("appkin-photo:a.jpg", "appkin-photo:b.jpg"), shared.photos)

        screen.onAction(share.action, darkMode)
        val textOnly = assertIs<ScreenViewModel.Effect.Share>(screen.effects.first())
        assertEquals("Dark mode", textOnly.text, "no empty line for the missing description")
        assertEquals(emptyList(), textOnly.photos)
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

    @Test
    fun anIdeaKeepsItsPhotosAndTheEditFormStartsWithThem() = runTest {
        val ideas = ideas()
        val screen = ideas.rainyScreen
        val photos = JsonArray(listOf(JsonPrimitive("appkin-photo:a.jpg"), JsonPrimitive("appkin-photo:b.jpg")))
        ideas.rainyNew.forms.set(ideas.rainyNew.form(), ideas.rainy, "photos", photos)
        ideas.rainyNew.add("title", "Tile", ideas.rainy)
        val tile = screen.items(ideas.rainy).first { it.getValue("title") == "Tile" }
        assertEquals(photos, tile["photos"])
        assertNull(ideas.rainyNew.forms.values(ideas.rainyNew.form(), ideas.rainy)["photos"], "the new-idea form starts without photos again")

        screen.onAction(screen.list().action, tile)
        val open = assertIs<ScreenViewModel.Effect.OpenScreen>(screen.effects.first())
        val edit = screen(AppData(store), "idea", open.payload)
        assertEquals(photos, edit.forms.values(edit.form(), open.payload)["photos"])

        edit.forms.set(edit.form(), open.payload, "photos", JsonArray(photos.take(1)))
        edit.onSubmit(edit.form(), open.payload)
        val saved = AppData(store).items(AppLibrary.idOf("App ideas"), "ideas").first { it.getValue("title") == "Tile" }
        assertEquals(JsonArray(photos.take(1)), saved["photos"])
    }

    @Test
    fun aRequiredPhotosFieldNeedsOne() {
        val field = PhotosInput(mandatory = true, title = "Photos", mapping = "photos")
        assertFalse(field.isSet(null))
        assertFalse(field.isSet(JsonArray(emptyList())))
        assertTrue(field.isSet(JsonArray(listOf(JsonPrimitive("appkin-photo:a.jpg")))))
        assertEquals(10, field.limit)
        assertEquals(1, PhotosInput(max = 0, title = "P", mapping = "p").limit)
    }

    @Test
    fun theUserChoosesTheOrderAndATagAboveTheIdeas() {
        var clock = 1_000L
        val appData = AppData(store, now = { clock++ })
        val ideas = ideas(appData)
        val (rainy, screen, new) = Triple(ideas.rainy, ideas.rainyScreen, ideas.rainyNew)
        // Dark mode (Medium) and Widgets (Low) are there; Tile is High and tagged.
        new.forms.set(new.form(), rainy, "priority", "High")
        new.forms.set(new.form(), rainy, "tags", JsonArray(listOf(JsonPrimitive("watch"), JsonPrimitive("UI"))))
        new.add("title", "Tile", rainy)
        val list = screen.list()
        val entry = screen.lists.get(list, rainy)
        assertEquals(listOf("Priority", "Newest", "Changed"), list.stored?.sortOptions?.map { it.title })

        assertEquals(listOf("Tile", "Dark mode", "Widgets"), screen.shown(rainy, "title"), "by priority first")
        screen.lists.choose(entry, ListChoice(sortOption = 1))
        assertEquals(listOf("Tile", "Widgets", "Dark mode"), screen.shown(rainy, "title"), "newest first")
        screen.onAction(list.swipeToEnd, screen.items(rainy).first { it.getValue("title") == "Tile" })
        assertEquals(listOf("Widgets", "Dark mode", "Tile"), screen.shown(rainy, "title"), "newest, but done ones last")

        screen.lists.choose(entry, ListChoice(sortOption = 2))
        assertEquals("Tile", screen.shown(rainy, "title").first(), "the one just changed first")

        assertEquals(listOf("UI", "watch"), entry.content.value.tags, "the tags in use, each once")
        screen.lists.choose(entry, ListChoice(sortOption = 2, tag = "ui"))
        assertEquals(listOf("Tile"), screen.shown(rainy, "title"), "only the tagged one, whatever the case")
        screen.lists.choose(entry, ListChoice(sortOption = 2, tag = null))
        assertEquals(3, screen.shown(rainy, "title").size)
    }

    @Test
    fun anIdeaShowsWhenItWasAddedAndChanged() {
        val now = PlaceholderClock.now()
        val ideas = ideas(AppData(store, now = { now - 3 * 60 * 60 * 1000 }))
        val item = ideas.rainyScreen.items(ideas.rainy).first()
        val added = ideas.rainyScreen.row().filterIsInstance<TextView>().first { it.title?.startsWith("Added") == true }
        assertEquals("Added 3 h ago", added.title!!.matchReplace(item))
        val changed = ideas.rainyScreen.row().filterIsInstance<TextView>().first { it.visibleIf == "updatedAt" }
        assertFalse(ViewVisibility.holds(changed.visibleIf, item), "not changed yet")
    }

    @Test
    fun theTagsFieldStartsFromAListOrFromTextWithCommas() {
        val field = TagsInput(title = "Tags", mapping = "tags")
        val values = InitialFormValuesUseCase()
        fun start(json: String) = values(FormView(formElements = listOf(field)), JsonText.parse(json) as JsonObject)["tags"]
        assertEquals(JsonArray(listOf(JsonPrimitive("a"), JsonPrimitive("b"))), start("""{ "tags": ["a", "b"] }"""))
        assertEquals(JsonArray(listOf(JsonPrimitive("watch"), JsonPrimitive("ui"))), start("""{ "tags": "watch, ui ," }"""))
        assertNull(start("""{ "title": "x" }"""))
        assertFalse(TagsInput(mandatory = true, title = "Tags", mapping = "tags").isSet(JsonArray(emptyList())))
    }
}
