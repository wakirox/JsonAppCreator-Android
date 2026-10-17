package it.sapienza.appinterpreter.model.event

/**
 * Opens the phone's calendar app on a new event, filled in from the configuration and the current data; the user
 * reviews and saves it there. [date] is dd/MM/yyyy and [time] HH:mm (none makes an all-day event); all text may use
 * ${field} placeholders.
 */
class CalendarEvent(
    val title: String,
    val date: String,
    val time: String?,
    val durationMinutes: Int?,
    val location: String?,
    val description: String?,
    mapping: String?,
    data: MutableMap<Any?, Any?>?
) : Event(mapping, data)
