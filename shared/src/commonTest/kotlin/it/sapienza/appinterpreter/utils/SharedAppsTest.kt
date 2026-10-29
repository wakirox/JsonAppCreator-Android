package it.sapienza.appinterpreter.utils

import it.sapienza.appinterpreter.Fixtures
import it.sapienza.appinterpreter.json.JsonText
import it.sapienza.appinterpreter.json.sameValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

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
        assertEquals("Tab x.appkin.json", SharedApps.fileName("Tab\tx\u0007"))
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
        assertEquals(emptyList(), SharedApps.webAddresses(app("Notes")))
        assertEquals(emptyList(), SharedApps.webAddresses("not json"))
    }

    @Test
    fun hostsAreReadAsJavaNetUriReadThem() {
        assertEquals("user.example.com", SharedApps.hostOf("https://ann@user.example.com:443/x"))
        assertEquals("[::1]", SharedApps.hostOf("http://[::1]:8080/"))
        assertEquals("10.0.2.2", SharedApps.hostOf("http://10.0.2.2/api"))
        assertEquals("example.com", SharedApps.hostOf(" HTTPS://Example.COM?q=1 "))
        // Not web addresses, or authorities java.net.URI gave no host for.
        assertNull(SharedApps.hostOf("ftp://example.com"))
        assertNull(SharedApps.hostOf("https://"))
        assertNull(SharedApps.hostOf("https://${'$'}{host}/x"))
        assertNull(SharedApps.hostOf("https://under_score.com/"))
        assertNull(SharedApps.hostOf("https://example.com:http/"))
        assertNull(SharedApps.hostOf("https://-bad.com/"))
    }

    @Test
    fun everyExampleListsItsWebsites() {
        val masterpieces = Fixtures.configurations.getValue("examples/v3_masterpieces.json")
        assertTrue(SharedApps.webAddresses(masterpieces).contains("commons.wikimedia.org"))
        val movies = Fixtures.configurations.getValue("raw/v3_the_movie_db.json")
        assertTrue(SharedApps.webAddresses(movies).contains("api.themoviedb.org"))
    }

    @Test
    fun arrivalComparesWithTheSavedAppOfThatName() {
        assertEquals(SharedApps.Arrival.New, SharedApps.arrival(app("Books"), null))
        // Same content, other formatting: nothing changes.
        assertEquals(
            SharedApps.Arrival.Unchanged,
            SharedApps.arrival(app("Books"), saved(app("Books").replace(" ", "")))
        )
        val other = saved(app("Books", version = "2"))
        assertEquals(SharedApps.Arrival.SameName(other), SharedApps.arrival(app("Books"), other))
    }

    @Test
    fun renamedKeepsEverythingButTheName() {
        val json = """{ "schemaVersion": 3, "name": "Books", "version": "1", "author": "Ann", """ +
            """"mainView": { "type": "text" } }"""
        val renamed = SharedApps.renamed(json, "Books 2")
        val tree = JsonText.parse(renamed) as JsonObject
        assertEquals("Books 2", tree.getValue("name").jsonPrimitive.content)
        assertTrue(JsonText.sameValue(JsonText.parse(json.replace("\"Books\"", "\"Books 2\"")), tree))
        // Keys keep their order, so the file still reads like the original.
        assertEquals(listOf("schemaVersion", "name", "version", "author", "mainView"), tree.keys.toList())
    }

    @Test
    fun renamedIsFormattedLikeTheEditorDoes() {
        assertEquals(
            "{\n  \"name\": \"B\",\n  \"price\": 0.10,\n  \"tags\": [ ]\n}",
            SharedApps.renamed("""{"name":"A","price":0.10,"tags":[]}""", "B")
        )
    }
}
