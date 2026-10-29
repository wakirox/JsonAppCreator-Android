package it.sapienza.appinterpreter.runtime.usecase

import it.sapienza.appinterpreter.json.JsonText
import it.sapienza.appinterpreter.model.event.Event
import it.sapienza.appinterpreter.model.event.EventSerializer
import it.sapienza.appinterpreter.model.event.PdfItems
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PdfPageUseCaseTest {
    private fun event(json: String) = assertIs<PdfItems>(Json.decodeFromString<Event>(EventSerializer, json))
    private fun obj(json: String) = JsonText.parse(json) as JsonObject

    private val pdf = event(
        """
        { "type": "pdf", "collection": "ideas", "where": { "done": false }, "sort": { "by": "title" },
          "title": "${'$'}{name}: open ideas", "text": "## ${'$'}{title}\n**${'$'}{priority}**\n\n${'$'}{description}",
          "photos": "photos", "emptyText": "Nothing" }
        """
    )
    private val app = obj("""{ "name": "Rainy" }""")

    @Test
    fun theTitleThenEachObjectsMarkdownWithItsPhotos() {
        val items = listOf(
            obj("""{ "title": "Dark mode", "priority": "High", "description": "For *night*", "photos": ["appkin-photo:a.jpg", "https://x.org/b.png"] }"""),
            obj("""{ "title": "Widgets", "priority": "Low" }""")
        )
        val page = PdfPageUseCase()(pdf, items, app, "App ideas")

        assertEquals("Rainy: open ideas", page.title)
        assertTrue("<h1>Rainy: open ideas</h1>" in page.html)
        assertTrue("<h3>Dark mode</h3>" in page.html, "an object's ## sits below the page's title")
        assertTrue("<p><strong>High</strong></p>" in page.html)
        assertTrue("<p>For <em>night</em></p>" in page.html)
        assertTrue("<img src=\"appkin-photo:a.jpg\"><img src=\"https://x.org/b.png\">" in page.html)
        assertEquals(2, Regex("<section>").findAll(page.html).count())
        assertEquals(listOf("appkin-photo:a.jpg"), page.photos, "only kept photos need the phone to fill them in")
        assertFalse("<div class=\"photos\"></div>" in page.html)
    }

    @Test
    fun aPagelessPdfIsOnePageAsTallAsItsMain() {
        assertFalse(PdfPageUseCase()(pdf, emptyList(), app, "App ideas").pageless, "A4 pages by default")
        val page = PdfPageUseCase()(event("""{ "type": "pdf", "collection": "c", "text": "x", "pageless": true }"""), emptyList(), null, "A")
        assertTrue(page.pageless)
        assertTrue("<body><main>" in page.html && "</main></body>" in page.html, "the phone measures the main")
    }

    @Test
    fun savedTextCantAddHtml() {
        val page = PdfPageUseCase()(pdf, listOf(obj("""{ "title": "<script>x</script>", "description": "a & \"b\"" }""")), app, "App ideas")
        assertFalse("<script>" in page.html)
        assertTrue("&lt;script&gt;x&lt;/script&gt;" in page.html)
        assertTrue("a &amp; &quot;b&quot;" in page.html)
    }

    @Test
    fun listsLinksAndTheAppsNameWithoutATitle() {
        val event = event("""{ "type": "pdf", "collection": "c", "text": "- one\n- [two](https://two.org)\n1. first\n- [bad](javascript:x)" }""")
        val page = PdfPageUseCase()(event, listOf(obj("{}")), null, "App ideas")
        assertEquals("App ideas", page.title)
        assertTrue("<ul>\n<li>one</li>\n<li><a href=\"https://two.org\">two</a></li>\n</ul>\n<ol>\n<li value=\"1\">first</li>\n</ol>" in page.html)
        assertTrue("<li>bad</li>" in page.html, "only web and mail links are kept")
    }
}
