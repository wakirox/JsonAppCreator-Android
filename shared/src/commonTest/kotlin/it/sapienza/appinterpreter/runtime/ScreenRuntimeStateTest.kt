package it.sapienza.appinterpreter.runtime

import it.sapienza.appinterpreter.json.getValue
import it.sapienza.appinterpreter.model.view_model.TextView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class ScreenRuntimeStateTest {
    private fun data(vararg pairs: Pair<String, Any>) = JsonObject(
        pairs.associate { (key, value) ->
            key to
                if (value is Number) JsonPrimitive(value) else JsonPrimitive(value.toString())
        }
    )

    @Test
    fun payloadIsUsedInsteadOfTheDefinitionsData() {
        val definition = TextView(id = "details", data = data("name" to "configured"))

        val screen = ScreenRuntimeState.create(definition, data("name" to "first"))

        assertEquals("first", screen.data?.getValue("name"))
        assertEquals("configured", definition.data?.getValue("name"))
    }

    @Test
    fun screensUsingTheSameDefinitionKeepSeparatePayloads() {
        val definition = TextView(id = "details")

        val first = ScreenRuntimeState.create(definition, data("id" to 1))
        val second = ScreenRuntimeState.create(definition, data("id" to 2))

        assertEquals("1", first.data?.getValue("id"))
        assertEquals("2", second.data?.getValue("id"))
        assertNull(definition.data)
    }

    @Test
    fun configuredDataIsUsedWhenNavigationHasNoPayload() {
        val definition = TextView(id = "about", data = data("title" to "About"))

        val screen = ScreenRuntimeState.create(definition, null)

        assertEquals("About", screen.data?.getValue("title"))
        assertNull(screen.navigationPayload)
    }
}
