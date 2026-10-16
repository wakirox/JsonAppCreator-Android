package it.sapienza.androidratio.appratio

import it.sapienza.appinterpreter.editor.ConfigurationChecker
import it.sapienza.appinterpreter.editor.SchemaSuggestions
import it.sapienza.appinterpreter.editor.Snippets
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import it.sapienza.appinterpreter.validation.ConfigurationValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class EditorAssistTest {
    private val schemaPath = Path.of("..", "schema", "v3.schema.json")
    private val checker by lazy {
        ConfigurationChecker(Files.newInputStream(schemaPath).use { ConfigurationValidator.fromSchema(it) })
    }
    private val suggestions by lazy { SchemaSuggestions(Files.readString(schemaPath)) }

    /** The text with "|" marking the cursor, and the labels suggested there. */
    private fun labelsAt(textWithCursor: String): List<String> {
        val cursor = textWithCursor.indexOf('|')
        return suggestions.at(textWithCursor.removeRange(cursor, cursor + 1), cursor).map { it.label }
    }

    @Test
    fun aValidConfigurationHasNoIssues() {
        assertEquals(emptyList<Any>(), checker.check(HELLO))
    }

    @Test
    fun aSchemaIssueIsPlacedAtItsKey() {
        val json = HELLO.replace("\"type\": \"text\"", "\"type\": \"txet\"")
        val issue = checker.check(json).single()
        assertEquals("$.mainView.views[0].type", issue.path)
        assertEquals(json.indexOf("\"type\": \"txet\""), issue.offset)
    }

    @Test
    fun aSyntaxErrorIsPlacedWhereTheParserStopped() {
        val json = HELLO.replace("\"label\": \"Hello\"", "\"label\" \"Hello\"")
        val issue = checker.check(json).single()
        val offset = checkNotNull(issue.offset)
        val label = json.indexOf("\"label\"")
        assertTrue("offset $offset should be at the missing colon", offset in label..json.indexOf("\"Hello\"", label))
    }

    @Test
    fun aMissingReferenceIsReported() {
        val json = HELLO.replace("\"label\": \"Hello\"", "\"label\": \"Hello\", \"action\": { \"ref\": \"nowhere\" }")
        val issue = checker.check(json).single()
        assertTrue(issue.message, issue.message.contains("nowhere"))
    }

    @Test
    fun rootKeysAreSuggestedExceptThoseAlreadyThere() {
        val labels = labelsAt("{\n  \"schemaVersion\": 3,\n  |\n}")
        assertTrue(labels.containsAll(listOf("name", "version", "mainView", "views", "actions")))
        assertTrue("schemaVersion" !in labels)
    }

    @Test
    fun aNewViewOffersTypeAndTheTypesAreItsValues() {
        assertTrue("type" in labelsAt("""{ "mainView": { "type": "layout", "views": [ { | } ] } }"""))
        assertEquals(
            listOf("image"),
            labelsAt("""{ "mainView": { "type": "layout", "views": [ { "type": "im|" } ] } }""")
        )
    }

    @Test
    fun aViewOffersThePropertiesOfItsType() {
        val labels = labelsAt("""{ "mainView": { "type": "layout", "views": [ { "type": "image", | } ] } }""")
        assertTrue(labels.containsAll(listOf("url", "size", "action")))
        assertTrue("views" !in labels && "textStyle" !in labels)
    }

    @Test
    fun aPartialKeyIsCompletedWithItsValueTemplate() {
        val text = """{ "mainView": { "type": "layout", "views": [ { "type": "text", "textSt" } ] } }"""
        val cursor = text.indexOf("textSt") + "textSt".length
        val suggestion = suggestions.at(text, cursor).single()
        assertEquals("textStyle", suggestion.label)
        assertEquals("\"textStyle\": \"\"", suggestion.insert)
        assertEquals(text.indexOf("\"textSt\""), suggestion.start)
        assertEquals(text.indexOf("\"textSt\"") + "\"textSt\"".length, suggestion.end)
    }

    @Test
    fun formFieldsAndEventsAreToldApartByType() {
        assertTrue(
            "options" in labelsAt("""{ "mainView": { "type": "form", "formElements": [ { "type": "selector", | } ] } }""")
        )
        assertEquals(
            listOf("show"),
            labelsAt("""{ "actions": [ { "id": "a", "event": { "type": "sh|" } } ] }""")
        )
        assertTrue(
            "thenDoOK" in labelsAt("""{ "actions": [ { "id": "a", "event": { "type": "alert", | } } ] }""")
        )
    }

    @Test
    fun anUnclosedStringIsExplainedInPlainWords() {
        val json = HELLO.replace("\"label\": \"Hello\"", "\"label\": \"Hello")
        assertEquals(
            "A text value isn't closed: add a quote (\") before the end of the line",
            checker.check(json).single().message
        )
    }

    @Test
    fun enterKeepsTheIndentationAndOpensBraces() {
        fun typeEnter(text: String): TextFieldValue {
            val cursor = text.indexOf('|')
            val before = text.removeRange(cursor, cursor + 1)
            val old = TextFieldValue(before, TextRange(cursor))
            return Snippets.withAutoIndent(old, TextFieldValue(before.replaceRange(cursor, cursor, "\n"), TextRange(cursor + 1)))
        }
        val plain = typeEnter("{\n    \"a\": 1,|\n}")
        assertEquals("{\n    \"a\": 1,\n    \n}", plain.text)
        assertEquals(plain.text.indexOf(",\n") + 6, plain.selection.start)

        val braces = typeEnter("  \"views\": [|]")
        assertEquals("  \"views\": [\n    \n  ]", braces.text)
        assertEquals(braces.text.indexOf("[\n") + 6, braces.selection.start)
    }

    @Test
    fun theNewAppTemplateAndEverySnippetAreValidWhereTheyGo() {
        assertEquals(emptyList<Any>(), checker.check(Snippets.newApp))
        Snippets.all.forEach { snippet ->
            val json = if (snippet.formField) {
                HELLO.replace(
                    "{ \"type\": \"text\", \"label\": \"Hello\" }",
                    "{ \"type\": \"form\", \"formElements\": [ ${snippet.json} ] }"
                )
            } else HELLO.replace("{ \"type\": \"text\", \"label\": \"Hello\" }", snippet.json)
            assertEquals(snippet.label, emptyList<Any>(), checker.check(json))
        }
    }

    private companion object {
        val HELLO = """
            {
              "schemaVersion": 3,
              "name": "Hello",
              "version": "1.0",
              "mainView": {
                "type": "layout",
                "views": [
                  { "type": "text", "label": "Hello" }
                ]
              }
            }
        """.trimIndent()
    }
}
