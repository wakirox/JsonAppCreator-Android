package it.sapienza.appinterpreter.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** The dd/MM/yyyy dates of forms and calendar events, read strictly as SimpleDateFormat did (lenient = false). */
object FormDates {
    const val DATE_PATTERN = "dd/MM/yyyy"

    /** [epochMillis] as dd/MM/yyyy in the phone's time zone. */
    fun format(epochMillis: Long, zone: TimeZone = TimeZone.currentSystemDefault()): String =
        format(Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone).date)

    fun format(date: LocalDate): String =
        "${date.day.toString().padStart(2, '0')}/${date.month.number.toString().padStart(2, '0')}/${date.year.toString().padStart(4, '0')}"

    const val TIME_PATTERN = "HH:mm"

    private val TIME = Regex("""^([01]?\d|2[0-3]):([0-5]\d)$""")

    /** The hour and minute of [text], HH:mm on a 24-hour clock (the hour may have one digit), or null. */
    fun parseTime(text: String): Pair<Int, Int>? =
        TIME.find(text.trim())?.destructured?.let { (hour, minute) -> hour.toInt() to minute.toInt() }

    /** [hour] and [minute] as HH:mm, as a time field submits them. */
    fun formatTime(hour: Int, minute: Int): String =
        "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

    private val DATE = Regex("""^(\d+)/(\d+)/(\d+)""")
    private val DATE_TIME = Regex("""^(\d+)/(\d+)/(\d+) (\d+):(\d+)""")

    /**
     * The day in [text], dd/MM/yyyy, or null. As SimpleDateFormat parsed it: one or more digits per field, a valid
     * day of that month, and anything after the date ignored.
     */
    fun parseDate(text: String): LocalDate? {
        val match = DATE.find(text) ?: return null
        val (day, month, year) = match.destructured
        return date(day, month, year)
    }

    /** The day and time in [text], "dd/MM/yyyy HH:mm", or null; anything after the time is ignored. */
    fun parseDateTime(text: String): LocalDateTime? {
        val match = DATE_TIME.find(text) ?: return null
        val (day, month, year, hour, minute) = match.destructured
        val date = date(day, month, year) ?: return null
        val h = hour.toIntOrNull()?.takeIf { it in 0..23 } ?: return null
        val m = minute.toIntOrNull()?.takeIf { it in 0..59 } ?: return null
        return LocalDateTime(date.year, date.month, date.day, h, m)
    }

    /** Midnight of [text]'s day in [zone], in epoch milliseconds, or null. */
    fun startOfDayMillis(text: String, zone: TimeZone): Long? =
        parseDate(text)?.let { LocalDateTime(it.year, it.month, it.day, 0, 0).toInstant(zone).toEpochMilliseconds() }

    private fun date(day: String, month: String, year: String): LocalDate? {
        val d = day.toIntOrNull() ?: return null
        val m = month.toIntOrNull()?.takeIf { it in 1..12 } ?: return null
        val y = year.toIntOrNull() ?: return null
        return runCatching { LocalDate(y, m, d) }.getOrNull()
    }
}
