package it.sapienza.appinterpreter.utils

import it.sapienza.appinterpreter.model.Layout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AppParserReferenceTest {
    @Test
    fun acceptsSchemaVersionThreeAndLegacyConfigsWithoutIt() {
        val current = AppParser(
            applicationJson(
                """
            "schemaVersion": 3,
            "mainView": { "type": "text", "label": "Hello" }
            """
            )
        ).parseApplication2()
        val legacy = parse(""""mainView": { "type": "text", "label": "Hello" }""")

        assertEquals(3, current.schemaVersion)
        assertEquals(null, legacy.schemaVersion)
    }

    @Test
    fun rejectsUnsupportedSchemaVersionAtItsJsonPath() {
        val error = invalid(
            """
            "schemaVersion": 4,
            "mainView": { "type": "text", "label": "Hello" }
            """
        )

        assertTrue(error.message.orEmpty().contains("$.schemaVersion"))
        assertTrue(error.message.orEmpty().contains("expected 3"))
    }

    @Test
    fun idBearingDefinitionIsNotMistakenForReference() {
        val application = parse(
            """
            "mainView": {
              "id": "welcome",
              "type": "text",
              "label": "Hello"
            }
            """
        )

        assertFalse(application.mainView.isReference())
        assertSame(application.mainView, application.viewBy("welcome"))
    }

    @Test
    fun resolvesPreferredExplicitViewAndActionReferences() {
        val application = parse(
            """
            "mainView": {
              "type": "layout",
              "views": [{ "ref": "target" }, {
                "type": "button",
                "title": "Open",
                "action": { "ref": "open" }
              }]
            },
            "views": [{ "id": "target", "type": "text", "label": "Target" }],
            "actions": [{
              "id": "open",
              "type": "click",
              "event": { "type": "show", "view": { "ref": "target" } }
            }]
            """
        )

        val children = (application.mainView as Layout).views
        assertSame(application.viewBy("target"), application.viewBy(children[0]))
        assertSame(application.actions.single(), application.actionBy(children[1].action!!))
    }

    @Test
    fun keepsLegacyIdOnlyViewAndActionReferencesWorking() {
        val application = parse(
            """
            "mainView": {
              "type": "layout",
              "views": [{ "id": "target" }, {
                "type": "button",
                "title": "Open",
                "action": { "id": "open" }
              }]
            },
            "views": [{ "id": "target", "type": "text", "label": "Target" }],
            "actions": [{
              "id": "open",
              "event": { "type": "show", "view": { "id": "target" } }
            }]
            """
        )

        val children = (application.mainView as Layout).views
        assertEquals("target", application.viewBy(children[0])?.id)
        assertEquals("open", application.actionBy(children[1].action!!)?.id)
    }

    @Test
    fun registersInlineDefinitionsForLaterReferences() {
        val application = parse(
            """
            "mainView": {
              "type": "layout",
              "views": [{
                "id": "inline-target",
                "type": "text",
                "label": "Target",
                "action": {
                  "id": "inline-action",
                  "event": { "type": "alert", "value": "Selected" }
                }
              }, {
                "ref": "inline-target"
              }, {
                "type": "button",
                "title": "Select",
                "action": { "ref": "inline-action" }
              }]
            }
            """
        )

        val children = (application.mainView as Layout).views
        assertSame(children[0], application.viewBy(children[1]))
        assertSame(children[0].action, application.actionBy(children[2].action!!))
    }

    @Test
    fun reportsUnresolvedViewAtItsJsonPath() {
        val error = invalid(
            """
            "mainView": { "type": "layout", "views": [{ "ref": "missing" }] }
            """
        )

        assertTrue(error.message.orEmpty().contains("$.mainView.views[0].ref"))
        assertTrue(error.message.orEmpty().contains("unresolved view reference 'missing'"))
    }

    @Test
    fun reportsUnresolvedActionAtItsJsonPath() {
        val error = invalid(
            """
            "mainView": {
              "type": "button",
              "title": "Open",
              "action": { "ref": "missing" }
            }
            """
        )

        assertTrue(error.message.orEmpty().contains("$.mainView.action.ref"))
        assertTrue(error.message.orEmpty().contains("unresolved action reference 'missing'"))
    }

    @Test
    fun rejectsDuplicateViewIdsWithBothLocations() {
        val error = invalid(
            """
            "mainView": { "id": "duplicate", "type": "text", "label": "One" },
            "views": [{ "id": "duplicate", "type": "text", "label": "Two" }]
            """
        )

        assertTrue(error.message.orEmpty().contains("duplicate view id 'duplicate'"))
        assertTrue(error.message.orEmpty().contains("first defined at $.views[0]"))
    }

    @Test
    fun rejectsDuplicateActionIds() {
        val error = invalid(
            """
            "mainView": { "type": "text", "label": "Hello" },
            "actions": [{
              "id": "duplicate",
              "event": { "type": "alert", "value": "One" }
            }, {
              "id": "duplicate",
              "event": { "type": "alert", "value": "Two" }
            }]
            """
        )

        assertTrue(error.message.orEmpty().contains("$.actions[1].id"))
        assertTrue(error.message.orEmpty().contains("duplicate action id 'duplicate'"))
    }

    @Test
    fun rejectsMixedReferenceAndDefinitionFields() {
        val error = invalid(
            """
            "mainView": { "ref": "target", "type": "text" },
            "views": [{ "id": "target", "type": "text", "label": "Target" }]
            """
        )

        assertTrue(error.message.orEmpty().contains("reference and definition fields cannot be mixed"))
    }

    @Test
    fun rejectsStructuralReferenceCycles() {
        val error = invalid(
            """
            "mainView": { "ref": "a" },
            "views": [{
              "id": "a",
              "type": "layout",
              "views": [{ "ref": "b" }]
            }, {
              "id": "b",
              "type": "layout",
              "views": [{ "ref": "a" }]
            }]
            """
        )

        assertTrue(error.message.orEmpty().contains("view containment cycle detected: a -> b -> a"))
    }

    private fun parse(body: String) = AppParser(applicationJson(body)).parseApplication2()

    private fun invalid(body: String): ConfigurationValidationException = try {
        parse(body)
        throw AssertionError("Expected configuration validation to fail")
    } catch (error: ConfigurationValidationException) {
        error
    }

    private fun applicationJson(body: String): String =
        """
        {
          "name": "Reference test",
          "author": "Appkin",
          "version": "1.0",
          $body
        }
        """.trimIndent()
}
