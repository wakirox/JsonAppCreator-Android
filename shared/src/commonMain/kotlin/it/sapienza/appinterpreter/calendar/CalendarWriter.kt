package it.sapienza.appinterpreter.calendar

/** An event to add to the phone's calendar: [beginMillis] and [endMillis] are instants, all-day ones UTC midnights. */
data class CalendarEntry(
    val title: String,
    val beginMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val location: String?,
    val description: String?
)

/**
 * Opens the phone's calendar on a new, filled-in event (Android: the calendar app's insert screen; iOS would use
 * EventKitUI). Nothing is saved without the user: no calendar permission is needed.
 */
interface CalendarWriter {
    /** False when the phone has no calendar app to open. */
    fun add(entry: CalendarEntry): Boolean
}
