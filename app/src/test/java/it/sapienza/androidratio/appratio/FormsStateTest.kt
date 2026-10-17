package it.sapienza.androidratio.appratio

import androidx.lifecycle.SavedStateHandle
import it.sapienza.appinterpreter.model.view_model.FormView
import it.sapienza.appinterpreter.runtime.FormsState
import it.sapienza.appinterpreter.utils.AppParser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FormsStateTest {
    private val form = AppParser(
        """
        {
          "name": "Forms", "version": "1.0", "schemaVersion": 3,
          "mainView": {
            "type": "form", "id": "signup",
            "formElements": [
              { "type": "text", "mapping": "runner.name", "title": "Name", "mandatory": true },
              { "type": "checkbox", "mapping": "rules", "title": "Rules", "mandatory": true }
            ]
          }
        }
        """
    ).parseApplication2().mainView as FormView
    private val data = JSONObject("""{ "runner": { "name": "Alex" } }""")
    private val saved = SavedStateHandle()

    @Test
    fun startsFromTheScreensData() {
        val values = FormsState(saved).values(form, data)

        assertEquals("Alex", values["runner.name"])
        assertEquals(false, values["rules"])
    }

    @Test
    fun aRecreatedScreenGetsTheTypedValuesBack() {
        FormsState(saved).set(form, data, "runner.name", "Giulia")

        // A new FormsState on the same SavedStateHandle: the screen after Android closed the app in the background.
        assertEquals("Giulia", FormsState(saved).values(form, data)["runner.name"])
    }

    @Test
    fun submittingSaysWhatIsMissingThenSendsNestedJson() {
        val forms = FormsState(saved)

        assertNull(forms.submit(form, data))
        assertEquals("Please complete: Rules", forms.error(form, data))

        forms.set(form, data, "rules", true)
        val json = forms.submit(form, data)
        assertNotNull(json)
        assertEquals("Alex", json!!.getJSONObject("runner").getString("name"))
        assertEquals(true, json.getBoolean("rules"))
        assertNull(forms.error(form, data))
    }
}
