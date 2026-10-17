package it.sapienza.appinterpreter.runtime.usecase

import android.content.Intent
import android.provider.CalendarContract
import it.sapienza.appinterpreter.extensions.matchReplace
import it.sapienza.appinterpreter.model.event.CalendarEvent
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * The calendar app's new-event screen, filled in from a calendar event and its data. Nothing is written to the
 * calendar here: the user saves the event in their calendar app, so no calendar permission is needed.
 */
class CreateCalendarEventUseCase {

    sealed interface Result {
        class Ready(val intent: Intent) : Result
        /** The date or time isn't dd/MM/yyyy and HH:mm. */
        class BadDate(val message: String) : Result
    }

    operator fun invoke(event: CalendarEvent, data: JSONObject?): Result {
        val date = event.date.matchReplace(data)
        val time = event.time?.matchReplace(data)?.takeIf { it.isNotBlank() }
        // A timed event starts at that local time; an all-day one covers that day from UTC midnight to the next,
        // as the calendar provider defines all-day events.
        val start = runCatching {
            SimpleDateFormat(if (time != null) "$DATE_PATTERN HH:mm" else DATE_PATTERN, Locale.ENGLISH).apply {
                isLenient = false
                if (time == null) timeZone = TimeZone.getTimeZone("UTC")
            }.parse(if (time != null) "$date $time" else date)
        }.getOrNull() ?: return Result.BadDate(
            "The event's date and time must be dd/MM/yyyy and HH:mm, but they are \"$date${time?.let { " $it" }.orEmpty()}\"."
        )
        val end = start.time + if (time != null) (event.durationMinutes ?: 60) * 60_000L else 24 * 3_600_000L
        val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, event.title.matchReplace(data))
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start.time)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
            .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, time == null)
        event.location?.let { intent.putExtra(CalendarContract.Events.EVENT_LOCATION, it.matchReplace(data)) }
        event.description?.let { intent.putExtra(CalendarContract.Events.DESCRIPTION, it.matchReplace(data)) }
        return Result.Ready(intent)
    }

    private companion object {
        /** The date format of a form's date field, which a calendar event's date uses too. */
        const val DATE_PATTERN = "dd/MM/yyyy"
    }
}
