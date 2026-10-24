package it.sapienza.appinterpreter.json

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonObject
import kotlin.time.Clock
import kotlin.time.Instant

/** The clock and time zone placeholder formats read: the phone's, or a test's. */
object PlaceholderClock {
    var now: () -> Long = { Clock.System.now().toEpochMilliseconds() }
    var zone: () -> TimeZone = { TimeZone.currentSystemDefault() }
}

/**
 * A placeholder's value in this data: "path", as getValue gives it, or "path|format" for a time in epoch milliseconds
 * (such as a saved object's createdAt or updatedAt): "|date" writes the day, dd/MM/yyyy as date fields do, and "|ago"
 * how long ago it was ("just now", "5 min ago", "3 h ago", "yesterday", "4 days ago", then the day). A value that
 * isn't a time stays as it is; null when the path has no value.
 */
fun JsonObject?.placeholderValue(placeholder: String): String? {
    val path = placeholder.substringBefore('|').trim()
    val format = placeholder.substringAfter('|', "").trim().takeIf { it.isNotEmpty() }
    val value = this?.getValue(path) ?: return null
    val millis = value.toDoubleOrNull()?.toLong() ?: return value
    return when (format) {
        "date" -> day(millis)
        "ago" -> ago(millis)
        else -> value
    }
}

private fun localDate(millis: Long): LocalDate =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(PlaceholderClock.zone()).date

private fun day(millis: Long): String = localDate(millis).let { date ->
    "${date.day.toString().padStart(2, '0')}/${date.month.ordinal.plus(1).toString().padStart(2, '0')}/${date.year}"
}

private fun ago(millis: Long): String {
    val now = PlaceholderClock.now()
    val minutes = (now - millis) / 60_000
    val days = localDate(millis).daysUntil(localDate(now))
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 24 * 60 -> "${minutes / 60} h ago"
        days <= 1 -> "yesterday"
        days < 7 -> "$days days ago"
        else -> day(millis)
    }
}
