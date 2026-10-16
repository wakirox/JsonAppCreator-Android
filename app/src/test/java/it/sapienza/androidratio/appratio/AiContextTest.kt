package it.sapienza.androidratio.appratio

import it.sapienza.appinterpreter.ai.AiContext
import it.sapienza.appinterpreter.editor.ConfigurationChecker
import it.sapienza.appinterpreter.editor.EditorIssue
import it.sapienza.appinterpreter.validation.ConfigurationValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

/**
 * The AI context: complete, with valid examples, and kept in docs/AI_CONTEXT.md.
 * After changing the guide, the schema or the examples, regenerate it with
 * `./gradlew :app:testDebugUnitTest --tests '*AiContextTest' -PupdateAiContext=true`.
 */
class AiContextTest {
    private val schema = Files.readString(Path.of("..", "schema", "v3.schema.json"))
    private val checker by lazy { ConfigurationChecker(ConfigurationValidator.fromSchema(schema.byteInputStream())) }
    private fun raw(name: String) = Files.readString(Path.of("src", "main", "res", "raw", "$name.json"))

    private val context by lazy {
        AiContext.build(
            guide = Files.readString(Path.of("..", "docs", "LLM_GUIDE.md")),
            schemaJson = schema,
            examples = AiContext.examples(raw("v3_masterpieces"), raw("v3_race_signup"), raw("v3_pokedex"))
        )
    }

    @Test
    fun theContextDescribesEveryViewFieldAndEvent() {
        listOf("layout", "text", "button", "image", "list", "form", "selector", "date", "checkbox", "rest", "show", "alert")
            .forEach { assertTrue("missing #### `$it`", context.contains("#### `$it`")) }
        assertTrue(context.contains("## Common mistakes"))
        assertTrue("repository-only sections are left out", !context.contains("./gradlew"))
    }

    @Test
    fun everyExampleInTheContextIsValid() {
        val examples = context.substring(context.indexOf("## Example 1"))
        val blocks = Regex("```json\\n(.*?)```", RegexOption.DOT_MATCHES_ALL).findAll(examples).map { it.groupValues[1] }.toList()
        assertEquals(3, blocks.size)
        blocks.forEach { assertEquals(emptyList<EditorIssue>(), checker.check(it)) }
    }

    @Test
    fun theDocsCopyIsUpToDate() {
        val docs = Path.of("..", "docs", "AI_CONTEXT.md")
        if (System.getProperty("updateAiContext") == "true") Files.writeString(docs, context)
        assertEquals(
            "docs/AI_CONTEXT.md is out of date: regenerate it (see this test's comment)",
            context,
            if (Files.exists(docs)) Files.readString(docs) else ""
        )
    }

    @Test
    fun theJsonIsFoundInAnAnswer() {
        val json = "{ \"a\": 1 }"
        assertEquals(json, AiContext.extractJson("Here you go:\n```json\n$json\n```\nEnjoy!"))
        assertEquals(json, AiContext.extractJson("Sure! $json Hope it helps."))
        assertNull(AiContext.extractJson("No JSON here"))
        // Placeholders that lost their $ get it back; complete ones are left alone.
        assertEquals(
            "{ \"url\": \"${'$'}{thumb}\", \"title\": \"${'$'}{author}\\n${'$'}{genre}, ${'$'}{year}\" }",
            AiContext.extractJson("{ \"url\": \"{thumb}\", \"title\": \"{author}\\n{genre}, ${'$'}{year}\" }")
        )
        // Markdown escapes added when copying from a chat app are undone.
        assertEquals("{ \"a\": [1, 2] }", AiContext.extractJson("{ \"a\": \\[1, 2\\] }"))
    }

    @Test
    fun aFixRequestListsTheProblemsWithLinesAndPaths() {
        val json = "{\n  \"name\": 1\n}"
        val request = AiContext.fixRequest(json, listOf(EditorIssue(4, "$.name", "must be a string")))
        assertTrue(request, request.contains("- line 2, $.name: must be a string"))
        assertTrue(request.contains(json))
    }

    @Test
    fun aRequestAndTheCurrentAppEndTheContext() {
        val withApp = AiContext.build("## Rules\n\nBe nice.\n", schema, emptyList(), currentApp = "{}", request = "Add a map")
        assertTrue(withApp.contains("## The current configuration"))
        assertTrue(withApp.contains("Change the current configuration: Add a map"))
    }
}
