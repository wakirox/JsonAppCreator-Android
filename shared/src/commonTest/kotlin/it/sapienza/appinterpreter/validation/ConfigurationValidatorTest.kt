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

    @Test
    fun openAndShareEventsAreChecked() {
        fun issues(event: String) = validator.validate(
            """{ "schemaVersion": 3, "name": "Events", "version": "1.0",
                 "mainView": { "type": "button", "title": "Go", "action": { "id": "go", "event": $event } } }"""
        ).issues.map { it.path }

        listOf(
            """{ "type": "open", "url": "https://clevelandart.org/art/${'$'}{id}" }""",
            """{ "type": "open", "url": "${'$'}{url}" }""",
            """{ "type": "open", "url": "geo:0,0?q=${'$'}{address}", "mapping": "location" }""",
            """{ "type": "open", "url": "tel:+12164217350" }""",
            """{ "type": "open", "url": "mailto:info@example.com" }""",
            """{ "type": "share", "text": "${'$'}{title} ${'$'}{url}" }"""
        ).forEach { assertEquals(emptyList(), issues(it), it) }
        assertEquals(listOf("$.mainView.action.event.url"), issues("""{ "type": "open", "url": "javascript:alert(1)" }"""))
        assertEquals(listOf("$.mainView.action.event"), issues("""{ "type": "open" }"""))
        assertEquals(listOf("$.mainView.action.event.text"), issues("""{ "type": "share", "text": "" }"""))
    }

    @Test
    fun selectorOptionsTimeAndHiddenFieldsAreChecked() {
        fun issues(fields: String) = validator.validate(
            """{ "schemaVersion": 3, "name": "Fields", "version": "1.0",
                 "mainView": { "type": "form", "formElements": [ $fields ] } }"""
        ).issues.map { it.path }.distinct()

        assertEquals(
            emptyList(),
            issues(
                """{ "type": "selector", "mapping": "type", "title": "Type", "options": [ { "label": "Any type", "value": "" }, "Painting", { "value": "Print" } ] },
                   { "type": "time", "mapping": "time", "title": "Time", "value": "9:30", "mandatory": true },
                   { "type": "hidden", "mapping": "id", "value": "${'$'}{id}" },
                   { "type": "hidden", "mapping": "title" }"""
            )
        )
        assertEquals(listOf("$.mainView.formElements[0].options[0]"), issues("""{ "type": "selector", "mapping": "t", "title": "T", "options": [ { "label": "No value" } ] }"""))
        assertEquals(listOf("$.mainView.formElements[0].value"), issues("""{ "type": "time", "mapping": "t", "title": "T", "value": "25:00" }"""))
        assertEquals(listOf("$.mainView.formElements[0]"), issues("""{ "type": "hidden", "mapping": "id", "title": "Shown?" }"""))
    }

    @Test
    fun gridsEmptyTextAndPagingAreChecked() {
        fun issues(list: String) = validator.validate(
            """{ "schemaVersion": 3, "name": "Lists", "version": "1.0", "mainView": $list }"""
        ).issues.map { it.path }.distinct()
        fun list(extra: String) = """{ "type": "list", "mapping": "data", "itemView": { "type": "text" }, $extra }"""

        assertEquals(emptyList(), issues(list(""""orientation": "grid", "columns": 3, "emptyText": "Nothing for ${'$'}{q}" """)))
        assertEquals(emptyList(), issues(list(""""initData": { "url": "https://x/a?limit=20", "nextPage": { "parameter": "skip", "step": 20 } }""")))
        assertEquals(emptyList(), issues(list(""""initData": { "url": "https://x/a", "nextPage": { "urlFrom": "info.next" } }""")))
        assertEquals(listOf("$.mainView.columns"), issues(list(""""orientation": "grid", "columns": 7""")))
        // initData is a request or null, so the request's issue comes with one for initData itself.
        assertTrue("$.mainView.initData.nextPage" in issues(list(""""initData": { "url": "https://x/a", "nextPage": { "parameter": "skip", "step": 20, "urlFrom": "next" } }""")))
        assertTrue("$.mainView.initData.nextPage" in issues(list(""""initData": { "url": "https://x/a", "nextPage": { "parameter": "skip" } }""")))
    }
}
