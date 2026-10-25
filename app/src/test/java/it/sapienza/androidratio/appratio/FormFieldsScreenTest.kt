package it.sapienza.androidratio.appratio

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import it.sapienza.appinterpreter.activity.MainActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A form shows option labels and its time, sends option values and its hidden fields, and shows nothing of those. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w448dp-h900dp-mdpi")
class FormFieldsScreenTest {
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferences = targetContext.getSharedPreferences("DomainController", Context.MODE_PRIVATE)
    private var original: String? = null
    private lateinit var scenario: ActivityScenario<MainActivity>

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Before
    fun setUp() {
        original = preferences.getString(KEY, null)
        preferences.edit().putString(KEY, CONFIGURATION).commit()
        scenario = ActivityScenario.launch(Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    @After
    fun tearDown() {
        scenario.close()
        preferences.edit().apply { if (original == null) remove(KEY) else putString(KEY, original) }.commit()
    }

    @Test
    fun labelsAreShownAndValuesSent() {
        composeRule.onNodeWithText("11:00").assertExists()
        composeRule.onNodeWithText("111654").assertDoesNotExist()
        composeRule.onNodeWithText("Painting").performClick()
        composeRule.onNodeWithText("Send").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("type=Painting id=111654 at 11:00").assertExists()
    }

    @Test
    fun anEmptyValueIsAChoiceToo() {
        composeRule.onNodeWithText("Any type").performClick()
        composeRule.onNodeWithText("Send").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("type= id=111654 at 11:00").assertExists()
    }

    private companion object {
        const val KEY = "main_file_sav"
        val CONFIGURATION = """
            {
              "schemaVersion": 3, "name": "Form fields test", "version": "1.0",
              "mainView": {
                "type": "layout",
                "data": { "id": 111654 },
                "views": [
                  { "type": "form", "buttonTitle": "Send",
                    "formElements": [
                      { "type": "selector", "mapping": "type", "title": "Type", "mandatory": true,
                        "options": [ { "label": "Any type", "value": "" }, "Painting" ] },
                      { "type": "time", "mapping": "time", "title": "Time", "value": "11:00" },
                      { "type": "hidden", "mapping": "id", "value": "${'$'}{id}" }
                    ],
                    "action": { "id": "send", "event": { "type": "alert", "value": "type=${'$'}{type} id=${'$'}{id} at ${'$'}{time}" } } }
                ]
              }
            }
        """.trimIndent()
    }
}
