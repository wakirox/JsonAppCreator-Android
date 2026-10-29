package it.sapienza.appinterpreter.editor

import it.sapienza.appinterpreter.json.JsonText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ImageLinksTest {
    private val app = """
        {
          "schemaVersion": 3, "name": "Trip", "version": "1.0",
          "mainView": {
            "type": "list",
            "stored": { "collection": "places", "initialItems": [
              { "name": "Arc", "photo": "https://example.org/made-up.jpg", "website": "https://arc.example" },
              { "name": "Park", "photo": "https://example.org/park.jpg" },
              { "name": "Again", "photo": "https://example.org/park.jpg" },
              { "name": "None", "photo": "" }
            ] },
            "itemView": { "type": "layout", "views": [
              { "type": "image", "url": "${'$'}{photo}" },
              { "type": "image", "url": "https://example.org/logo.png" },
              { "type": "image", "url": "https://example.org/${'$'}{id}.png" }
            ] }
          }
        }
    """.trimIndent()

    @Test
    fun anImageViewsAddressAndTheValuesOfTheFieldsImagesShowOnceEach() {
        val links = ImageLinks.find(JsonText.parse(app)!!)
        assertEquals(
            setOf(
                "$.mainView.itemView.views[1].url" to "https://example.org/logo.png",
                "$.mainView.stored.initialItems[0].photo" to "https://example.org/made-up.jpg",
                "$.mainView.stored.initialItems[1].photo" to "https://example.org/park.jpg"
            ),
            links.toSet()
        )
        assertEquals(3, links.size, "the same address once")
        assertTrue(links.none { "arc.example" in it.second }, "a website isn't an image")
    }

    @Test
    fun anImageThatDoesntLoadIsAProblemPlacedAtItsAddress() = runTest {
        val statuses = mapOf(
            "https://example.org/made-up.jpg" to 404,
            "https://example.org/park.jpg" to 200,
            "https://example.org/logo.png" to null
        )
        val problems = ImageLinks.problems(app) { statuses[it] }

        assertEquals(
            listOf("$.mainView.stored.initialItems[0].photo", "$.mainView.itemView.views[1].url"),
            problems.map {
                it.path
            }.sortedDescending()
        )
        val madeUp = problems.first { "made-up" in it.message }
        assertTrue(madeUp.message.startsWith("This image doesn't load (error 404): https://example.org/made-up.jpg"))
        assertEquals(app.indexOf("\"photo\": \"https://example.org/made-up.jpg\""), madeUp.offset)
        assertTrue(problems.first { "logo" in it.message }.message.contains("(no answer)"))
    }

    @Test
    fun anAppWithoutWebImagesOrUnreadableJsonHasNoImageProblems() = runTest {
        assertEquals(emptyList(), ImageLinks.problems("""{ "name": "x" }""") { error("not asked") })
        assertEquals(emptyList(), ImageLinks.problems("{ not json") { error("not asked") })
    }
}
