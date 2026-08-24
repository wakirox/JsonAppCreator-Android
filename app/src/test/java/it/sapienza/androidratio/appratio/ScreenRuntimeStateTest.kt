package it.sapienza.androidratio.appratio

import it.sapienza.appinterpreter.model.view_model.helper.MView
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
        val definition = MView(id = "details", data = mutableMapOf("name" to "configured"))
        val navigationPayload = JSONObject().put("name", "first")

        val screen = ScreenRuntimeState.create(definition, navigationPayload)
        navigationPayload.put("name", "changed by caller")

        assertEquals("first", screen.data?.getString("name"))
        assertEquals("configured", definition.dataObj?.getString("name"))
    }

    @Test
    fun screensUsingTheSameDefinitionKeepSeparatePayloads() {
        val definition = MView(id = "details")

        val first = ScreenRuntimeState.create(definition, JSONObject().put("id", 1))
        val second = ScreenRuntimeState.create(definition, JSONObject().put("id", 2))

        assertEquals(1, first.data?.getInt("id"))
        assertEquals(2, second.data?.getInt("id"))
        assertNull(definition.data)
    }

    @Test
    fun configuredDataIsUsedWhenNavigationHasNoPayload() {
        val definition = MView(id = "about", data = mutableMapOf("title" to "About"))

        val screen = ScreenRuntimeState.create(definition, null)

        assertEquals("About", screen.data?.getString("title"))
        assertNull(screen.navigationPayload)
    }
}
