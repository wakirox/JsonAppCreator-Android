package it.sapienza.androidratio.appratio

import com.fasterxml.jackson.databind.ObjectMapper
import it.sapienza.appinterpreter.utils.AppLibrary
import it.sapienza.appinterpreter.utils.SharedApps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Sharing an app as its JSON file: the file name, the websites it uses, and how it meets a saved app of that name. */
class SharedAppsTest {
    private fun app(name: String, version: String = "1") =
        """{ "schemaVersion": 3, "name": "$name", "version": "$version", "mainView": { "type": "text" } }"""

    private fun saved(json: String) = AppLibrary.SavedApp("app:books", "Books", json, 0, "file:1")

    @Test
    fun fileNameIsTheAppNameMadeSafe() {
        assertEquals("Shopping list.appkin.json", SharedApps.fileName("Shopping list"))
        assertEquals("Books a b.appkin.json", SharedApps.fileName("  Books/a:b  "))
        assertEquals("App.appkin.json", SharedApps.fileName("..."))
        assertEquals("App.appkin.json", SharedApps.fileName(" ?* "))
        assertEquals(60 + ".appkin.json".length, SharedApps.fileName("x".repeat(200)).length)
    }

    @Test
    fun webAddressesAreTheHostsOfEveryLink() {
        val json = """{
            "name": "Movies",
            "mainView": { "type": "image", "url": "https://Upload.Wikimedia.org/a.jpg",
                "views": [ { "url": "https://api.themoviedb.org/3/movie/${'$'}{id}?api_key=__TMDB_API_KEY__" },
                           { "url": "http://example.com:8080/x" }, { "text": "see https://upload.wikimedia.org/b" } ] }
        }"""
        assertEquals(listOf("api.themoviedb.org", "example.com", "upload.wikimedia.org"), SharedApps.webAddresses(json))
    }

    @Test
    fun anAppWithoutLinksUsesNoWebsite() {
        assertEquals(emptyList<String>(), SharedApps.webAddresses(app("Notes")))
        assertEquals(emptyList<String>(), SharedApps.webAddresses("not json"))
    }

    @Test
    fun everyExampleListsItsWebsites() {
        val masterpieces = File("../examples/v3_masterpieces.json").readText()
        assertTrue(SharedApps.webAddresses(masterpieces).contains("commons.wikimedia.org"))
    }

    @Test
    fun arrivalComparesWithTheSavedAppOfThatName() {
        assertEquals(SharedApps.Arrival.New, SharedApps.arrival(app("Books"), null))
        // Same content, other formatting: nothing changes.
        assertEquals(SharedApps.Arrival.Unchanged, SharedApps.arrival(app("Books"), saved(app("Books").replace(" ", ""))))
        val other = saved(app("Books", version = "2"))
        assertEquals(SharedApps.Arrival.SameName(other), SharedApps.arrival(app("Books"), other))
    }

    @Test
    fun renamedKeepsEverythingButTheName() {
        val json = """{ "schemaVersion": 3, "name": "Books", "version": "1", "author": "Ann", "mainView": { "type": "text" } }"""
        val renamed = SharedApps.renamed(json, "Books 2")
        val mapper = ObjectMapper()
        assertEquals("Books 2", mapper.readTree(renamed)["name"].asText())
        assertEquals(mapper.readTree(json.replace("\"Books\"", "\"Books 2\"")), mapper.readTree(renamed))
        // Keys keep their order, so the file still reads like the original.
        assertEquals(listOf("schemaVersion", "name", "version", "author", "mainView"), mapper.readTree(renamed).fieldNames().asSequence().toList())
    }
}
