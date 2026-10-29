package it.sapienza.androidratio.appratio

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import it.sapienza.appinterpreter.activity.MainActivity
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

/** Open events hand their address to the right kind of app; share events open the share sheet. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w448dp-h900dp-mdpi")
class OpenAndShareTest {
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

    private fun tap(button: String): Intent {
        composeRule.onNodeWithText(button).performClick()
        composeRule.waitForIdle()
        var started: Intent? = null
        scenario.onActivity { started = shadowOf(it).nextStartedActivity }
        return checkNotNull(started) { "$button started nothing" }
    }

    @Test
    fun aWebPageOpensInTheBrowser() {
        val intent = tap("Museum page")
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertTrue(intent.hasCategory(Intent.CATEGORY_BROWSABLE))
        assertEquals(Uri.parse("https://clevelandart.org/art/1930.189"), intent.data)
    }

    @Test
    fun aPlaceOpensAMap() {
        val intent = tap("Map")
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("geo:0,0?q=11150%20East%20Blvd", intent.dataString)
    }

    @Test
    fun aPhoneNumberOnlyFillsInTheDialer() {
        val intent = tap("Call")
        assertEquals(Intent.ACTION_DIAL, intent.action)
        assertEquals("+12164217350", intent.data?.schemeSpecificPart)
    }

    @Test
    fun anEmailAddressGoesToEmailApps() {
        val intent = tap("Email")
        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals("info@clevelandart.org", intent.data?.schemeSpecificPart)
    }

    @Test
    fun shareOpensTheShareSheetOnTheText() {
        val chooser = tap("Share")
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("text/plain", send.type)
        assertEquals(
            "South Wind, Clear Sky https://clevelandart.org/art/1930.189",
            send.getStringExtra(Intent.EXTRA_TEXT)
        )
    }

    private companion object {
        const val KEY = "main_file_sav"
        val CONFIGURATION = """
            {
              "schemaVersion": 3, "name": "Open test", "version": "1.0",
              "mainView": {
                "type": "layout",
                "data": { "title": "South Wind, Clear Sky", "url": "https://clevelandart.org/art/1930.189",
                          "address": "11150 East Blvd", "phone": "+12164217350", "email": "info@clevelandart.org" },
                "views": [
                  { "type": "button", "title": "Museum page", "action": { "id": "web", "event": { "type": "open", "url": "${'$'}{url}" } } },
                  { "type": "button", "title": "Map", "action": { "id": "map", "event": { "type": "open", "url": "geo:0,0?q=${'$'}{address}" } } },
                  { "type": "button", "title": "Call", "action": { "id": "call", "event": { "type": "open", "url": "tel:${'$'}{phone}" } } },
                  { "type": "button", "title": "Email", "action": { "id": "email", "event": { "type": "open", "url": "mailto:${'$'}{email}" } } },
                  { "type": "button", "title": "Share", "action": { "id": "share", "event": { "type": "share", "text": "${'$'}{title} ${'$'}{url}" } } }
                ]
              }
            }
        """.trimIndent()
    }
}
