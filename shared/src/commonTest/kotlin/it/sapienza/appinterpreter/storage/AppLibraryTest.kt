package it.sapienza.appinterpreter.storage

import it.sapienza.appinterpreter.security.ApiKeyPlaceholders
import it.sapienza.appinterpreter.utils.AppIcons
import it.sapienza.appinterpreter.utils.AppLibrary
import it.sapienza.appinterpreter.utils.BundledConfiguration
import it.sapienza.appinterpreter.utils.CurrentApp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Your apps and the current app, on an in-memory store. */
class AppLibraryTest {
    private var time = 1_000L
    private val store = MemoryAppStore()
    private val library = AppLibrary(store) { time++ }

    private fun app(name: String, version: String = "1") =
        """{ "schemaVersion": 3, "name": "$name", "version": "$version", "mainView": { "type": "text" } }"""

    @Test
    fun appsAreSavedByNameNewestFirstAndTheLatestReplacesAnEarlierOne() {
        library.save(app("Books"), "file:1")
        library.save(app("Kitchen"), "file:2")
        library.save(app("books", version = "2"), "file:3")
        assertEquals(listOf("books", "Kitchen"), library.all().map { it.name })
        assertEquals(app("books", version = "2"), library.all().first().json)
        assertEquals(setOf("app:books", "app:kitchen"), store.keys())
    }

    @Test
    fun entriesWrittenByTheOldAndroidCodeStillRead() {
        // org.json wrote slashes escaped, and an old entry may lack its configuration id.
        store.put("app:old", """{"name":"Old","json":"{\"url\":\"https:\/\/x\"}","savedAt":5}""")
        val old = library.get("app:old")!!
        assertEquals("{\"url\":\"https://x\"}", old.json)
        assertEquals("", old.configurationId)
        assertNull(library.get("missing"))
        store.put("app:broken", "not json")
        assertNull(library.get("app:broken"))
    }

    @Test
    fun aNewAppGetsAFreeNameAndNamelessAppsAreNotSaved() {
        assertEquals("My app", library.freeName("My app"))
        library.save(app("My app"), "file:1")
        library.save(app("My app 2"), "file:2")
        assertEquals("My app 3", library.freeName("My app"))
        assertNull(library.save("{ \"name\": \"  \" }", "file:3"))
        assertNull(library.save("not json", "file:4"))
        library.delete(AppLibrary.idOf("My app"))
        assertEquals(listOf("My app 2"), library.all().map { it.name })
    }

    @Test
    fun theCurrentAppKeepsItsIdentityAndExamplesAreNotSaved() {
        val current = CurrentApp(MemoryAppStore(), library) { BundledConfiguration("v3_masterpieces", app("Masterpieces")) }
        assertEquals(app("Masterpieces"), current.json())
        assertEquals("bundled:v3_masterpieces", current.configurationId())

        current.set(BundledConfiguration("v3_race_signup", app("Race")))
        assertEquals("bundled:v3_race_signup", current.configurationId())
        assertEquals(emptyList(), library.all())

        current.set(app("Planner"), "bundled:v3_planner")
        assertEquals("bundled:v3_planner", library.get(AppLibrary.idOf("Planner"))!!.configurationId)
        current.set(app("Notes"))
        assertEquals(ApiKeyPlaceholders.configurationId(app("Notes")), current.configurationId())
    }

    @Test
    fun iconsHaveALetterAndAColourFromTheName() {
        assertEquals("B", AppIcons.letterOf("  books"))
        assertEquals("?", AppIcons.letterOf("!!"))
        assertEquals(AppIcons.colorOf("Books"), AppIcons.colorOf("Books"))
        assertNotEquals(AppIcons.colorOf("Masterpieces"), AppIcons.colorOf("Race sign-up"))
        assertTrue(AppIcons.colorOf("x") ushr 24 == 0xFF)
    }
}
