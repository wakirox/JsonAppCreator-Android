package it.sapienza.androidratio.appratio

import android.content.Context
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
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
import org.robolectric.annotation.GraphicsMode

/** Three buttons with icons sharing a row on a 360 dp phone keep each label on one line (App ideas' top row). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-mdpi")
class RowButtonsTest {
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferences = targetContext.getSharedPreferences("DomainController", Context.MODE_PRIVATE)
    private var original: String? = null
    private var scenario: ActivityScenario<MainActivity>? = null

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Before
    fun setUp() {
        original = preferences.getString(KEY, null)
        preferences.edit().putString(KEY, CONFIGURATION).commit()
    }

    @After
    fun tearDown() {
        scenario?.close()
        preferences.edit().apply { if (original == null) remove(KEY) else putString(KEY, original) }.commit()
    }

    private fun lines(text: String): Int {
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(
            text
        ).fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!(layouts)
        return layouts.first().lineCount
    }

    @Test
    fun eachLabelInARowOfThreeButtonsStaysOnOneLine() {
        scenario =
            ActivityScenario.launch(
                Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        listOf("New idea", "PDF", "Export").forEach { assertEquals(it, 1, lines(it)) }
    }

    private companion object {
        const val KEY = "main_file_sav"
        val CONFIGURATION = """
            {
              "schemaVersion": 3,
              "name": "Row buttons",
              "version": "1.0.0",
              "mainView": {
                "type": "layout",
                "views": [
                  {
                    "type": "layout",
                    "orientation": "horizontal",
                    "views": [
                      { "type": "button", "title": "New idea", "icon": "add", "action": { "id": "new", "event": { "type": "back" } } },
                      { "type": "button", "title": "PDF", "icon": "share", "action": { "id": "pdf", "event": { "type": "back" } } },
                      { "type": "button", "title": "Export", "icon": "share", "action": { "id": "export", "event": { "type": "back" } } }
                    ]
                  }
                ]
              }
            }
        """.trimIndent()
    }
}
