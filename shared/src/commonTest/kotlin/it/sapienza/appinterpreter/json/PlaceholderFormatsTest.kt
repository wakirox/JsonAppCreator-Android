package it.sapienza.appinterpreter.json

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class PlaceholderFormatsTest {
    // Sunday 25 October 2026, 12:00 in Bucharest (UTC+2 that day, after the clocks went back).
    private val now = 1_792_922_400_000L
    private val minute = 60_000L
    private val hour = 60 * minute
    private val day = 24 * hour

    @BeforeTest
    fun fixTheClock() {
        PlaceholderClock.now = { now }
        PlaceholderClock.zone = { TimeZone.of("Europe/Bucharest") }
    }

    @AfterTest
    fun freeTheClock() {
        PlaceholderClock.now = { kotlin.time.Clock.System.now().toEpochMilliseconds() }
        PlaceholderClock.zone = { TimeZone.currentSystemDefault() }
    }

    private fun at(millis: Long) = JsonObject(
        mapOf(
            "createdAt" to JsonPrimitive(millis),
            "title" to JsonPrimitive("Tile")
        )
    )

    @Test
    fun aTimeReadsAsTheDay() {
        assertEquals("Created 25/10/2026", "Created ${'$'}{createdAt|date}".matchReplace(at(now)))
        assertEquals(
            "01/01/2026",
            "${'$'}{createdAt | date}".matchReplace(at(1_767_225_600_000L)),
            "spaces around the format are fine"
        )
    }

    @Test
    fun aTimeReadsAsHowLongAgo() {
        fun ago(millis: Long) = "${'$'}{createdAt|ago}".matchReplace(at(millis))
        assertEquals("just now", ago(now - 20_000))
        assertEquals("5 min ago", ago(now - 5 * minute))
        assertEquals("3 h ago", ago(now - 3 * hour))
        assertEquals("yesterday", ago(now - 30 * hour))
        assertEquals("4 days ago", ago(now - 4 * day))
        assertEquals("05/10/2026", ago(now - 20 * day), "after a week, the day")
    }

    @Test
    fun anythingElseStaysAsItIs() {
        assertEquals("Tile", "${'$'}{title|date}".matchReplace(at(now)), "not a time")
        assertEquals("", "${'$'}{updatedAt|ago}".matchReplace(at(now)), "no value, as any placeholder")
        assertEquals(
            "Created yesterday",
            "Created ${'$'}{createdAt|ago}\n${'$'}{updatedAt|ago}".filledLines(at(now - 30 * hour))
        )
    }
}
