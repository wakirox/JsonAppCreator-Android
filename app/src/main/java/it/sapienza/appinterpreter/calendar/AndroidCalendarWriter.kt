package it.sapienza.appinterpreter.calendar

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract

/** The [CalendarWriter] on Android: the calendar app's insert screen, which [context] (an activity) opens. */
class AndroidCalendarWriter(private val context: Context) : CalendarWriter {

    override fun add(entry: CalendarEntry): Boolean = try {
        context.startActivity(intent(entry))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    companion object {
        fun intent(entry: CalendarEntry): Intent {
            val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, entry.title)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, entry.beginMillis)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, entry.endMillis)
                .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, entry.allDay)
            entry.location?.let { intent.putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
            entry.description?.let { intent.putExtra(CalendarContract.Events.DESCRIPTION, it) }
            return intent
        }
    }
}
