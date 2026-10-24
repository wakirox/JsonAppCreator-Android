package it.sapienza.appinterpreter.validation

import it.sapienza.appinterpreter.Fixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigurationValidatorTest {
    private val validator = ConfigurationValidator.fromSchema(V3Schema.JSON)

    @Test
    fun everyBundledConfigurationIsValid() {
        assertTrue(Fixtures.configurations.size >= 10)
        Fixtures.configurations.forEach { (name, json) ->
            assertEquals(emptyList(), validator.validate(json).issues, name)
        }
    }

    @Test
    fun issuesHavePathsAndTheMessagesTheAppAlwaysShowed() {
        val issues = validator.validate(
            """
            {
              "schemaVersion": 4, "name": "", "version": "1.0",
              "mainView": {
                "type": "layout", "orientation": "diagonal",
                "views": [
                  { "type": "text", "colour": "red", "textSize": "big" },
                  { "type": "form", "formElements": [ { "type": "text" } ] },
                  { "type": "button", "action": { "id": "a", "event": { "type": "alert", "thenDoOK": { "type": "show" } } } }
                ]
              }
            }
            """.trimIndent()
        ).issues
        assertEquals(
            listOf(
                "$.mainView.orientation: does not have a value in the enumeration [\"horizontal\", \"vertical\", ]",
                "$.mainView.views[0]: property 'colour' is not defined in the schema and the schema does not allow additional properties",
                "$.mainView.views[0].textSize: string found, [number, null] expected",
                "$.mainView.views[1].formElements[0]: required property 'mapping' not found",
                "$.mainView.views[1].formElements[0]: required property 'title' not found",
                "$.mainView.views[2].action.event.thenDoOK: must be valid to one and only one schema, but 0 are valid",
                "$.mainView.views[2].action.event.thenDoOK: object found, null expected",
                "$.mainView.views[2].action.event.thenDoOK: required property 'view' not found",
                "$.name: must be at least 1 characters long",
                "$.schemaVersion: must be the constant value '3'"
            ),
            issues.map { "${it.path}: ${it.message}" }
        )
        assertTrue(issues.all { it.kind == ConfigurationIssueKind.SCHEMA })
    }

    @Test
    fun malformedOrEmptyJsonIsOneIssueAtTheRoot() {
        val broken = validator.validate("{\n  \"name\": \"Broken\",\n}").issues.single()
        assertEquals(ConfigurationIssueKind.MALFORMED_JSON, broken.kind)
        assertEquals("$", broken.path)
        assertTrue(broken.message.startsWith("Invalid JSON at line 3, column 1"), broken.message)
        assertEquals("Configuration is empty", validator.validate(" ").issues.single().message)
    }

    @Test
    fun pointersBecomeJsonPathsWithIndexesOnlyInArrays() {
        val document = it.sapienza.appinterpreter.json.JsonText.parse("""{ "a": [ { "0": { "b": 1 } } ] }""")!!
        assertEquals("$.a[0].0.b", ConfigurationValidator.jsonPath("/a/0/0/b", document))
        assertEquals("$", ConfigurationValidator.jsonPath("", document))
    }

    @Test
    fun everyViewMayHaveVisibleIfAndTextsShowWhenEmpty() {
        val views = listOf(
            """{ "type": "text", "label": "Artist", "mapping": "creators.0.description", "visibleIf": "creators", "showWhenEmpty": true }""",
            """{ "type": "button", "title": "Plan a visit", "visibleIf": "current_location" }""",
            """{ "type": "image", "url": "${'$'}{images.web.url}", "visibleIf": "images.web.url" }""",
            """{ "type": "list", "itemView": { "type": "text" }, "visibleIf": "!items" }""",
            """{ "type": "form", "formElements": [], "visibleIf": "id" }""",
            """{ "type": "layout", "views": [ { "ref": "shared", "visibleIf": "a.b" } ], "visibleIf": "x" }"""
        )
        val issues = validator.validate(
            """{ "schemaVersion": 3, "name": "Visible", "version": "1.0",
                 "mainView": { "type": "layout", "views": [ ${views.joinToString()} ] },
                 "views": [ { "id": "shared", "type": "text", "title": "Shared" } ] }"""
        ).issues
        assertEquals(emptyList(), issues)
    }

    @Test
    fun visibleIfIsAPathNotAnExpression() {
        listOf("a == b", "!!a", "!", "").forEach { condition ->
            val issues = validator.validate(
                """{ "schemaVersion": 3, "name": "Visible", "version": "1.0",
                     "mainView": { "type": "text", "title": "x", "visibleIf": "$condition" } }"""
            ).issues
            assertEquals(listOf("$.mainView.visibleIf"), issues.map { it.path }, condition)
        }
    }
}
