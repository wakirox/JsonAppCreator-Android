package it.sapienza.androidratio.appratio

import it.sapienza.appinterpreter.model.dataObj
import it.sapienza.appinterpreter.model.view_model.TextView
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import it.sapienza.appinterpreter.runtime.ScreenRuntimeState
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@Config(sdk = [35])
class ScreenRuntimeStateTest {

    @Test
    fun payloadIsIndependentFromDefinitionAndCaller() {
        val definition = TextView(id = "details", data = JsonObject(mapOf("name" to JsonPrimitive("configured"))))
        val navigationPayload = JSONObject().put("name", "first")

        val screen = ScreenRuntimeState.create(definition, navigationPayload)
        navigationPayload.put("name", "changed by caller")

        assertEquals("first", screen.data?.getString("name"))
        assertEquals("configured", definition.dataObj?.getString("name"))
    }

    @Test
    fun screensUsingTheSameDefinitionKeepSeparatePayloads() {
        val definition = TextView(id = "details")

        val first = ScreenRuntimeState.create(definition, JSONObject().put("id", 1))
        val second = ScreenRuntimeState.create(definition, JSONObject().put("id", 2))

        assertEquals(1, first.data?.getInt("id"))
        assertEquals(2, second.data?.getInt("id"))
        assertNull(definition.data)
    }

    @Test
    fun configuredDataIsUsedWhenNavigationHasNoPayload() {
        val definition = TextView(id = "about", data = JsonObject(mapOf("title" to JsonPrimitive("About"))))

        val screen = ScreenRuntimeState.create(definition, null)

        assertEquals("About", screen.data?.getString("title"))
        assertNull(screen.navigationPayload)
    }
}
