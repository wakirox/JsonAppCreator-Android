package it.sapienza.androidratio.appratio

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
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

/** Views whose data lacks what they show aren't drawn, and leave no gap. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w448dp-h900dp-mdpi")
class HiddenViewsTest {
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
    fun emptyAndHiddenViewsAreNotDrawn() {
        listOf("Technique", "Plan a visit", "Has no artist").forEach {
            composeRule.onNodeWithText(it, substring = true).assertDoesNotExist()
        }
        listOf("Dora Wheeler", "Artist", "Culture", "Visit us").forEach {
            composeRule.onNodeWithText(it, substring = true).assertExists()
        }
        composeRule.onNodeWithText("${'$'}{", substring = true).assertDoesNotExist()
    }

    @Test
    fun hiddenViewsTakeNoSpace() {
        // Between each pair a hidden view sits, at the screen's top level or inside a layout; the gaps are the same
        // as between two views with nothing between them.
        val gaps = listOf("First" to "Second", "Second" to "Third", "Third" to "Fourth", "Fourth" to "Fifth").map { (above, below) ->
            top(below) - bottom(above)
        }
        assertEquals(gaps.toString(), 1, gaps.distinct().size)
    }

    private fun top(text: String): Dp = composeRule.onNodeWithText(text).getUnclippedBoundsInRoot().top
    private fun bottom(text: String): Dp = composeRule.onNodeWithText(text).getUnclippedBoundsInRoot().bottom

    private companion object {
        const val KEY = "main_file_sav"
        val CONFIGURATION = """
            {
              "schemaVersion": 3, "name": "Hidden views", "version": "1.0",
              "mainView": {
                "type": "layout",
                "data": { "title": "Dora Wheeler", "creators": [ { "name": "Chase" } ], "culture": "", "current_location": null },
                "views": [
                  { "type": "text", "mapping": "title" },
                  { "type": "text", "label": "Artist", "mapping": "creators.0.name", "visibleIf": "creators" },
                  { "type": "text", "label": "Has no artist", "visibleIf": "!creators" },
                  { "type": "text", "label": "Technique", "mapping": "technique" },
                  { "type": "text", "label": "Culture", "mapping": "culture", "showWhenEmpty": true },
                  { "type": "button", "title": "Plan a visit", "visibleIf": "current_location" },
                  { "type": "text", "label": "Visit us" },
                  { "type": "text", "title": "First" },
                  { "type": "text", "label": "Made", "title": "${'$'}{creation_date}" },
                  { "type": "text", "title": "Second" },
                  { "type": "layout", "views": [
                    { "type": "text", "title": "Third" },
                    { "type": "image", "url": "https://example.com/a.jpg", "visibleIf": "images" },
                    { "type": "text", "title": "Fourth" }
                  ] },
                  { "type": "text", "title": "Hidden too", "visibleIf": "!title" },
                  { "type": "text", "title": "Fifth" }
                ]
              }
            }
        """.trimIndent()
    }
}
