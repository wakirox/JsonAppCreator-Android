package it.sapienza.androidratio.appratio

import it.sapienza.appinterpreter.model.dataObj
import it.sapienza.appinterpreter.activity.MainActivity
import it.sapienza.appinterpreter.activity.spawnInstance
import it.sapienza.appinterpreter.model.view_model.TextView
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@Config(sdk = [35])
class ScreenNavigationIntentTest {
    @Test
    fun navigationSerializesPayloadWithoutWritingToDefinition() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).get()
        val definition = TextView(id = "details", data = JsonObject(mapOf("kind" to JsonPrimitive("configured"))))
        val payload = JSONObject().put("kind", "runtime")

        spawnInstance(activity, definition, payload)

        val intent = shadowOf(activity).nextStartedActivity
        assertEquals("details", intent.getStringExtra("idView"))
        assertEquals("runtime", JSONObject(intent.getStringExtra("screenPayload")!!).getString("kind"))
        assertEquals("configured", definition.dataObj?.getString("kind"))
    }
}
