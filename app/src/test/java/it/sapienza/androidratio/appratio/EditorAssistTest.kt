package it.sapienza.androidratio.appratio

import it.sapienza.appinterpreter.editor.ConfigurationChecker
import it.sapienza.appinterpreter.editor.SchemaSuggestions
import it.sapienza.appinterpreter.editor.JsonColors
import it.sapienza.appinterpreter.editor.Snippets
import it.sapienza.appinterpreter.editor.highlightJson
import it.sapienza.appinterpreter.editor.prettyJson
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
        return suggestions.at(textWithCursor.removeRange(cursor, cursor + 1), cursor).items.map { it.label }
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
    fun anUnknownPropertyIsPlacedAtItsName() {
        val json = HELLO.replace("\"label\": \"Hello\"", "\"label\": \"Hello\", \"colour\": \"red\"")
        assertEquals(json.indexOf("\"colour\""), checker.check(json).single().offset)
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
        val suggestion = suggestions.at(text, cursor).items.single()
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
    fun requiredAttributesComeFirstAndAreMarked() {
        val text = "{\n  \"schemaVersion\": 3,\n  \n}"
        val result = suggestions.at(text, text.indexOf("  \n}") + 2)
        assertEquals("app", result.heading)
        assertEquals(listOf("name", "version", "mainView"), result.items.filter { it.required }.map { it.label })
        assertEquals(listOf("name", "version", "mainView"), result.items.take(3).map { it.label })

        val list = """{ "mainView": { "type": "list", | } }"""
        val cursor = list.indexOf('|')
        val listResult = suggestions.at(list.removeRange(cursor, cursor + 1), cursor)
        assertEquals("list", listResult.heading)
        assertEquals(listOf("itemView"), listResult.items.filter { it.required }.map { it.label })
    }

    @Test
    fun whileWritingAValueTheObjectsMissingAttributesAreAddedAtItsEnd() {
        val inline = """{ "mainView": { "type": "layout", "views": [ { "type": "image", "url": "https://x" } ] } }"""
        val inValue = inline.indexOf("https://x") + 3
        val result = suggestions.at(inline, inValue)
        assertEquals("image", result.heading)
        val size = result.items.first { it.label == "size" }
        val applied = inline.replaceRange(size.start, size.end, size.insert)
        assertTrue(applied, applied.contains("\"url\": \"https://x\", \"size\": {} }"))
        assertEquals(applied.indexOf("{} }") + 1, size.start + size.cursor)

        val multiline = "{\n  \"mainView\": {\n    \"type\": \"text\",\n    \"label\": \"Hi\"\n  }\n}"
        val label = suggestions.at(multiline, multiline.indexOf("Hi") + 1).items.first { it.label == "textSize" }
        assertEquals(
            "{\n  \"mainView\": {\n    \"type\": \"text\",\n    \"label\": \"Hi\",\n    \"textSize\": \n  }\n}",
            multiline.replaceRange(label.start, label.end, label.insert)
        )
    }

    @Test
    fun numbersAreRecognisedFromTheSchemaAndFromTypedDigits() {
        fun numberAt(textWithCursor: String): Boolean {
            val cursor = textWithCursor.indexOf('|')
            return suggestions.isNumberAt(textWithCursor.removeRange(cursor, cursor + 1), cursor)
        }
        assertTrue(numberAt("""{ "mainView": { "type": "text", "textSize": | } }"""))
        assertTrue(numberAt("""{ "mainView": { "type": "text", "textSize": 2|0 } }"""))
        assertTrue(numberAt("""{ "mainView": { "type": "image", "size": { "height": | } } }"""))
        assertTrue(!numberAt("""{ "mainView": { "type": "text", "label": "2|0" } }"""))
        assertTrue(!numberAt("""{ "mainView": { "type": "text", "label": | } }"""))
        assertTrue(!numberAt("""{ "mainView": { "type": "text", | } }"""))
        // On the next line the letters come back, for "}" or the next property.
        assertTrue(!numberAt("{ \"mainView\": { \"type\": \"text\", \"textSize\": 20\n|} }"))
        // Digits typed for a property the schema doesn't know (inside data) still get the number pad.
        assertTrue(numberAt("""{ "mainView": { "type": "text", "data": { "price": 1|9 } } }"""))
    }

    @Test
    fun formattingKeepsNumbersAndDuplicatesAndSurvivesLongStrings() {
        assertEquals("{\n  \"price\": 0.10,\n  \"big\": 12345678901234567890.5\n}", prettyJson("{\"price\":0.10,\"big\":12345678901234567890.5}"))
        val duplicated = "{\"a\": 1, \"a\": 2}"
        assertEquals("a key twice is left as it is", duplicated, prettyJson(duplicated))
        val long = "{\"data\": \"" + "x".repeat(200_000) + "\"}"
        assertEquals(4, highlightJson(long, JsonColors.Light).spanStyles.size) // { key string }
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
