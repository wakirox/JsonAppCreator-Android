package it.sapienza.androidratio.appratio

import android.content.Context
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import it.sapienza.appinterpreter.activity.MainActivity
import it.sapienza.appinterpreter.runtime.ScreenViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** A label's own style, HTML and Markdown text, and the title a show event gives the screen it opens. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w448dp-h900dp-mdpi")
class TextStylesAndTitlesTest {
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

    private fun launch(intent: Intent = Intent(targetContext, MainActivity::class.java)) {
        scenario = ActivityScenario.launch(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun shown(text: String): AnnotatedString =
        composeRule.onNodeWithText(text).fetchSemanticsNode().config[SemanticsProperties.Text].first()

    private fun AnnotatedString.weightAt(index: Int) = spanStyles.lastOrNull { index in it.start until it.end }?.item?.fontWeight
    private fun AnnotatedString.slantAt(index: Int) = spanStyles.lastOrNull { index in it.start until it.end }?.item?.fontStyle

    @Test
    fun htmlIsShownWithoutItsTagsAndTheLabelHasItsOwnStyle() {
        launch()
        val text = shown("About\nCole wrote Essay on American Scenery.")
        assertEquals(FontWeight.Bold, text.weightAt(0))
        val essay = text.text.indexOf("Essay")
        assertEquals(FontStyle.Italic, text.slantAt(essay))
        assertEquals(FontWeight.Normal, text.weightAt(text.text.indexOf("Cole")).let { it ?: FontWeight.Normal })
        assertTrue(text.text.indexOf("wrote") > 0)
    }

    @Test
    fun markdownIsShownWithoutItsMarksStyledAndWithItsLinks() {
        launch()
        val text = shown("Tips\nGo early\n\nBuy tickets at the site.\n\n•  Bring water\n•  Wear shoes")
        assertEquals(FontWeight.Bold, text.weightAt(text.text.indexOf("Go early")))
        assertEquals(FontWeight.Bold, text.weightAt(text.text.indexOf("tickets")))
        assertEquals(FontStyle.Italic, text.slantAt(text.text.indexOf("water")))
        val link = text.getLinkAnnotations(0, text.length).single().item as LinkAnnotation.Url
        assertEquals("https://example.com/t", link.url)
    }

    @Test
    fun withoutALabelStyleTheLabelIsAHeading() {
        launch()
        val text = shown("Plain\nwritten <b>as is</b>")
        assertEquals(FontWeight.SemiBold, text.weightAt(0))
        assertEquals("the text under it stays regular", null, text.weightAt(text.text.indexOf("written")))
    }

    @Test
    fun aShowEventsTitleReachesTheScreenItOpens() {
        launch()
        composeRule.onNodeWithText("Works").performClick()
        composeRule.waitForIdle()
        var started: Intent? = null
        scenario!!.onActivity { started = shadowOf(it).nextStartedActivity }
        assertEquals("Works by Hokusai", checkNotNull(started).getStringExtra(ScreenViewModel.EXTRA_SCREEN_TITLE))
        scenario!!.close()

        launch(checkNotNull(started))
        composeRule.onNodeWithText("Works by Hokusai").assertExists()
        composeRule.onNodeWithText("Text test").assertDoesNotExist()
    }

    private companion object {
        const val KEY = "main_file_sav"
        val CONFIGURATION = """
            {
              "schemaVersion": 3, "name": "Text test", "version": "1.0",
              "mainView": {
                "type": "layout",
                "data": { "name": "Hokusai", "description": "<p>Cole wrote <em>Essay on American Scenery</em>.</p>" },
                "views": [
                  { "type": "text", "label": "About", "labelStyle": "bold", "mapping": "description", "format": "html" },
                  { "type": "text", "label": "Plain", "title": "written <b>as is</b>" },
                  { "type": "text", "label": "Tips", "format": "markdown", "title": "## Go early\nBuy **tickets** at [the site](https://example.com/t).\n\n- Bring *water*\n- Wear shoes" },
                  { "type": "button", "title": "Works", "action": { "id": "works", "event": { "type": "show", "view": { "ref": "works" }, "title": "Works by ${'$'}{name}" } } }
                ]
              },
              "views": [ { "id": "works", "type": "text", "title": "Their works" } ]
            }
        """.trimIndent()
    }
}
