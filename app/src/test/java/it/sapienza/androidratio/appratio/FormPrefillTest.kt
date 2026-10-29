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

/** Values from the screen's data fill a form only when they fit its fields. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w448dp-h900dp-mdpi")
class FormPrefillTest {
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
        scenario =
            ActivityScenario.launch(
                Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
    }

    @After
    fun tearDown() {
        scenario.close()
        preferences.edit().apply { if (original == null) remove(KEY) else putString(KEY, original) }.commit()
    }

    @Test
    fun aValueThatIsNotAnOptionOrIsNullDoesNotCountAsFilledIn() {
        composeRule.onNodeWithText("Send").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Please complete: Size, Email").assertExists()
    }

    @Test
    fun aDateInAnotherFormatIsShownAsWritten() {
        composeRule.onNodeWithText("1990-05-01").assertExists()
    }

    private companion object {
        const val KEY = "main_file_sav"
        val CONFIGURATION = """
            {
              "schemaVersion": 3, "name": "Prefill test", "version": "1.0",
              "mainView": {
                "type": "layout",
                "data": { "size": "XL", "email": null, "birth": "1990-05-01" },
                "views": [
                  { "type": "form", "buttonTitle": "Send", "formElements": [
                    { "type": "selector", "mapping": "size", "title": "Size", "options": ["S", "M"], "mandatory": true },
                    { "type": "text", "mapping": "email", "title": "Email", "mandatory": true },
                    { "type": "date", "mapping": "birth", "title": "Birth", "mandatory": true }
                  ] }
                ]
              }
            }
        """.trimIndent()
    }
}
