package it.sapienza.appinterpreter.runtime.usecase

import it.sapienza.appinterpreter.json.JsonText
import it.sapienza.appinterpreter.model.event.Event
import it.sapienza.appinterpreter.model.event.EventSerializer
import it.sapienza.appinterpreter.model.event.TextPackItems
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TextPackUseCaseTest {
    private fun obj(json: String) = JsonText.parse(json) as JsonObject
    private val event = assertIs<TextPackItems>(
        Json.decodeFromString<Event>(
            EventSerializer,
            """{ "type": "textpack", "collection": "ideas", "title": "${'$'}{name}: open ideas", "text": "## ${'$'}{title}\n${'$'}{description}", "photos": "photos", "emptyText": "Nothing yet" }"""
        )
    )

    @Test
    fun theTitleThenEachObjectWithItsPhotosAsAssetsNamedAfterIt() {
        val items = listOf(
            obj("""{ "title": "Dark mode!", "description": "For night", "photos": ["appkin-photo:a.jpg", "appkin-photo:b.jpg", "https://x.org/c.png"] }"""),
            obj("""{ "title": "Dark mode", "photos": "appkin-photo:d.jpg" }"""),
            obj("""{ "description": "No title" }""")
        )
        val pack = TextPackUseCase()(event, items, obj("""{ "name": "Rainy" }"""), "App ideas")

        assertEquals("Rainy: open ideas", pack.title)
        assertEquals(
            listOf("assets/dark-mode-1.jpg" to "appkin-photo:a.jpg", "assets/dark-mode-2.jpg" to "appkin-photo:b.jpg", "assets/dark-mode-1-2.jpg" to "appkin-photo:d.jpg"),
            pack.assets.map { it.path to it.reference },
            "named after the object, never twice the same"
        )
        assertEquals(
            """
            # Rainy: open ideas

            ## Dark mode!
            For night

            ![](assets/dark-mode-1.jpg)

            ![](assets/dark-mode-2.jpg)

            ![](https://x.org/c.png)

            ---

            ## Dark mode

            ![](assets/dark-mode-1-2.jpg)

            ---

            No title
            """.trimIndent() + "\n",
            pack.markdown
        )
    }

    @Test
    fun withoutObjectsTheEmptyTextAndTheAppsNameWithoutATitle() {
        val untitled = assertIs<TextPackItems>(Json.decodeFromString<Event>(EventSerializer, """{ "type": "textpack", "collection": "c", "text": "x", "emptyText": "Nothing yet" }"""))
        val pack = TextPackUseCase()(untitled, emptyList(), null, "App ideas")
        assertEquals("# App ideas\n\nNothing yet\n", pack.markdown)
        assertEquals(emptyList(), pack.assets)
    }

    @Test
    fun aSlugIsLowerCaseLettersAndDigitsJoinedByHyphens() {
        assertEquals("città-2026-plan", TextPackUseCase.slug("Città 2026 — plan!"))
        assertEquals("", TextPackUseCase.slug("!!"))
    }
}
