package it.sapienza.appinterpreter.model.event

import it.sapienza.appinterpreter.model.TypedSerializer
import it.sapienza.appinterpreter.model.view_model.helper.MView
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** What an action does, chosen by its "type". */
@Serializable(with = EventSerializer::class)
abstract class Event {
    abstract val mapping: String?
    abstract val data: JsonObject?
}

@Serializable
class AlertMessage(
    val value: String? = null,
    val thenDoOK: Event? = null,
    val thenDoKO: Event? = null,
    override val mapping: String? = null,
    override val data: JsonObject? = null
) : Event()

/**
 * Opens the phone's calendar app on a new event, filled in from the configuration and the current data; the user
 * reviews and saves it there. [date] is dd/MM/yyyy and [time] HH:mm (none makes an all-day event); all text may use
 * ${field} placeholders.
 */
@Serializable
class CalendarEvent(
    val title: String,
    val date: String,
    val time: String? = null,
    @Serializable(with = LenientIntSerializer::class) val durationMinutes: Int? = null,
    val location: String? = null,
    val description: String? = null,
    override val mapping: String? = null,
    override val data: JsonObject? = null
) : Event()

/** Fills lists with data from a service, then runs [thenDo]. */
abstract class DataService : Event() {
    abstract val thenDo: Event?
}

@Serializable
class RESTService(
    val url: String,
    val method: CallServiceMethod? = CallServiceMethod.get,
    override val thenDo: Event? = null,
    override val mapping: String? = null,
    override val data: JsonObject? = null
) : DataService()

@Serializable
class ShowView(
    val view: MView,
    override val mapping: String? = null,
    override val data: JsonObject? = null
) : Event()

object EventSerializer : TypedSerializer<Event>(
    "Event",
    listOf(
        TypedSerializer.Type("rest", RESTService.serializer()) { it is RESTService },
        TypedSerializer.Type("alert", AlertMessage.serializer()) { it is AlertMessage },
        TypedSerializer.Type("show", ShowView.serializer()) { it is ShowView },
        TypedSerializer.Type("calendar", CalendarEvent.serializer()) { it is CalendarEvent }
    )
)

@Serializable
enum class CallServiceMethod {
    get, post
}
