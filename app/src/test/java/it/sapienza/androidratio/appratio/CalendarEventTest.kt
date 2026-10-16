package it.sapienza.androidratio.appratio

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import it.sapienza.appinterpreter.activity.MainActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** A calendar event opens the calendar app's new-event screen, filled in from the data. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w448dp-h900dp-mdpi")
class CalendarEventTest {
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
    fun aTimedEventOpensTheCalendarFilledIn() {
        composeRule.onNodeWithText("Add dinner").performClick()
        composeRule.waitForIdle()
        var started: Intent? = null
        scenario.onActivity { started = shadowOf(it).nextStartedActivity }
        val intent = checkNotNull(started)
        assertEquals(Intent.ACTION_INSERT, intent.action)
        assertEquals(CalendarContract.Events.CONTENT_URI, intent.data)
        assertEquals("Dinner with Ana", intent.getStringExtra(CalendarContract.Events.TITLE))
        val begin = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ROOT).parse("20/10/2026 19:30")!!.time
        assertEquals(begin, intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, 0))
        assertEquals(begin + 150 * 60_000L, intent.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, 0))
        assertFalse(intent.getBooleanExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true))
        assertEquals("6 guests", intent.getStringExtra(CalendarContract.Events.DESCRIPTION))
    }

    @Test
    fun withoutATimeItCoversThatDayInUtc() {
        composeRule.onNodeWithText("Add day").performClick()
        composeRule.waitForIdle()
        var started: Intent? = null
        scenario.onActivity { started = shadowOf(it).nextStartedActivity }
        val intent = checkNotNull(started)
        assertTrue(intent.getBooleanExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false))
        val utcMidnight = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC") }
            .parse("20/10/2026")!!.time
        assertEquals(utcMidnight, intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, 0))
        assertEquals(utcMidnight + 24 * 3_600_000L, intent.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, 0))
    }

    @Test
    fun aMappingSelectsTheDataOfTheEvent() {
        composeRule.onNodeWithText("Add race").performClick()
        composeRule.waitForIdle()
        var started: Intent? = null
        scenario.onActivity { started = shadowOf(it).nextStartedActivity }
        assertEquals("Summer Run", checkNotNull(started).getStringExtra(CalendarContract.Events.TITLE))
    }

    private companion object {
        const val KEY = "main_file_sav"
        val CONFIGURATION = """
            {
              "schemaVersion": 3, "name": "Calendar test", "version": "1.0",
              "mainView": {
                "type": "layout",
                "data": { "who": "Ana", "when": "20/10/2026", "guests": "6", "race": { "name": "Summer Run", "day": "13/06/2027" } },
                "views": [
                  { "type": "button", "title": "Add dinner", "action": { "id": "dinner", "event": {
                    "type": "calendar", "title": "Dinner with ${'$'}{who}", "date": "${'$'}{when}", "time": "19:30",
                    "durationMinutes": 150, "description": "${'$'}{guests} guests" } } },
                  { "type": "button", "title": "Add day", "action": { "id": "day", "event": {
                    "type": "calendar", "title": "Day off", "date": "${'$'}{when}" } } },
                  { "type": "button", "title": "Add race", "action": { "id": "race", "event": {
                    "type": "calendar", "mapping": "race", "title": "${'$'}{name}", "date": "${'$'}{day}" } } }
                ]
              }
            }
        """.trimIndent()
    }
}
