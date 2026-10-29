package it.sapienza.androidratio.appratio

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import it.sapienza.appinterpreter.activity.MainActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w448dp-h900dp-mdpi")
class FormRequiredFieldsTest {

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferences =
        targetContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private var hadOriginalConfiguration = false
    private var originalConfiguration: String? = null
    private lateinit var scenario: ActivityScenario<MainActivity>

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Before
    fun setUp() {
        hadOriginalConfiguration = preferences.contains(CONFIGURATION_KEY)
        originalConfiguration = preferences.getString(CONFIGURATION_KEY, null)
        preferences.edit().putString(CONFIGURATION_KEY, CONFIGURATION).commit()
        scenario = ActivityScenario.launch(
            Intent(targetContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    @After
    fun tearDown() {
        scenario.close()
        if (hadOriginalConfiguration) {
            preferences.edit().putString(CONFIGURATION_KEY, originalConfiguration).commit()
        } else {
            preferences.edit().remove(CONFIGURATION_KEY).commit()
        }
    }

    @Test
    fun uncheckedRequiredCheckboxBlocksSubmission() {
        composeRule.onNodeWithText("Sign up").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Please complete: I accept the rules").assertExists()
        composeRule.onNodeWithText("Welcome", substring = true).assertDoesNotExist()
    }

    @Test
    fun prefilledValuesCountAsFilledInAndAreSubmitted() {
        composeRule.onNodeWithText("I accept the rules").performClick()
        composeRule.onNodeWithText("Sign up").performClick()
        composeRule.waitForIdle()

        assertEquals(
            0,
            composeRule.onAllNodesWithText("Please complete", substring = true)
                .fetchSemanticsNodes().size
        )
        composeRule.onNodeWithText("Welcome, Alex (10 km)").assertExists()
    }

    companion object {
        private const val PREFERENCES_NAME = "DomainController"
        private const val CONFIGURATION_KEY = "main_file_sav"
        private val CONFIGURATION = """
            {
              "schemaVersion": 3,
              "name": "Required fields test",
              "version": "1.0",
              "mainView": {
                "type": "layout",
                "data": { "name": "Alex", "distance": "10 km" },
                "views": [
                  {
                    "type": "form",
                    "buttonTitle": "Sign up",
                    "action": { "ref": "confirm" },
                    "formElements": [
                      { "type": "text", "mapping": "name", "title": "Name", "mandatory": true },
                      { "type": "text", "mapping": "nickname", "title": "Nickname" },
                      { "type": "selector", "mapping": "distance", "title": "Distance", "mandatory": true,
                        "options": ["5 km", "10 km"] },
                      { "type": "checkbox", "mapping": "rules", "title": "I accept the rules", "mandatory": true }
                    ]
                  }
                ]
              },
              "actions": [
                { "id": "confirm", "type": "click",
                  "event": { "type": "alert", "value": "Welcome, ${'$'}{name} (${'$'}{distance})" } }
              ]
            }
        """.trimIndent()
    }
}
