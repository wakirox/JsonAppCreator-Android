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

/**
 * Hands [url] to the phone: a web page opens in the browser, geo: a map, tel: the dialer (nothing is called) and
 * mailto: an email app. Placeholders in it are filled in from the current data.
 */
@Serializable
class OpenLink(
    val url: String,
    override val mapping: String? = null,
    override val data: JsonObject? = null
) : Event()

/** Opens the phone's share sheet on [text], filled in from the current data. */
@Serializable
class ShareText(
    val text: String,
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
    override val data: JsonObject? = null,
    /** A list's initData only: how to ask for the items after these. */
    val nextPage: NextPage? = null,
    /** Request headers, such as "Authorization"; values may use ${field} placeholders and __API_KEY__ ones. */
    val headers: Map<String, String>? = null,
    /** An event's only: what runs when the request fails, with the data and "error" ({ message, status }). */
    val onError: Event? = null
) : DataService()

/**
 * Saves the current data (the part [mapping] selects, if any) as a new object of the app's [collection] on the phone,
 * with [fields] added, then runs [thenDo] with the saved object. Its ${field} placeholders are filled from the data
 * the form showed (for a form: the screen's or the row's), then from the submitted values.
 */
@Serializable
class SaveData(
    val collection: String,
    val fields: JsonObject? = null,
    val thenDo: Event? = null,
    override val mapping: String? = null,
    override val data: JsonObject? = null
) : Event()

/**
 * How a list asks for more items: either the query [parameter] (such as "skip") grows by [step], the page size,
 * from its value in the first URL (0 without one), until a page has fewer than [step] items; or the next page's URL
 * is the response's value at [urlFrom], until there is none.
 */
@Serializable
class NextPage(
    val parameter: String? = null,
    val step: Int? = null,
    val urlFrom: String? = null
)

@Serializable
class ShowView(
    val view: MView,
    override val mapping: String? = null,
    override val data: JsonObject? = null,
    /** The opened screen's title in the app bar, instead of the app's name; placeholders come from its data. */
    val title: String? = null
) : Event()

object EventSerializer : TypedSerializer<Event>(
    "Event",
    listOf(
        TypedSerializer.Type("rest", RESTService.serializer()) { it is RESTService },
        TypedSerializer.Type("alert", AlertMessage.serializer()) { it is AlertMessage },
        TypedSerializer.Type("show", ShowView.serializer()) { it is ShowView },
        TypedSerializer.Type("calendar", CalendarEvent.serializer()) { it is CalendarEvent },
        TypedSerializer.Type("open", OpenLink.serializer()) { it is OpenLink },
        TypedSerializer.Type("share", ShareText.serializer()) { it is ShareText },
        TypedSerializer.Type("save", SaveData.serializer()) { it is SaveData }
    )
)

@Serializable
enum class CallServiceMethod {
    get, post, put, delete;

    /** Whether the request sends its data as a JSON body. */
    val sendsBody: Boolean get() = this == post || this == put
}
