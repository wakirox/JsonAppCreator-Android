package it.sapienza.appinterpreter.runtime.usecase

import it.sapienza.appinterpreter.json.fillAddress
import it.sapienza.appinterpreter.json.matchReplace
import it.sapienza.appinterpreter.json.missingPlaceholders
import it.sapienza.appinterpreter.json.missingAddressPlaceholders
import it.sapienza.appinterpreter.model.event.CallServiceMethod
import it.sapienza.appinterpreter.model.event.RESTService
import it.sapienza.appinterpreter.utils.httpclient.JsonHttpClient
import kotlinx.serialization.json.JsonObject

/**
 * A request event's JSON answer: its URL filled in from the data (each value percent-encoded, see fillAddress), which
 * a POST also sends as its body. A URL whose placeholder has no value isn't requested: it fails with
 * [MissingValueException] instead.
 */
class FetchJsonUseCase(private val client: JsonHttpClient) {
    suspend operator fun invoke(event: RESTService, data: JsonObject?): JsonObject = fetch(url(event, data), event, data)

    /** The URL [event] asks for with [data]. */
    fun url(event: RESTService, data: JsonObject?): String =
        event.url.fillAddress(data) ?: throw MissingValueException(event.url.missingAddressPlaceholders(data).first())

    /** [url], already filled in (such as a list's next page), asked for as [event] says, with its headers. */
    suspend fun fetch(url: String, event: RESTService, data: JsonObject?): JsonObject =
        client.request(url, event.method ?: CallServiceMethod.get, body = data, headers = headers(event, data))

    /** [event]'s headers filled in from [data]; like the URL, a header needing a value [data] lacks isn't sent. */
    fun headers(event: RESTService, data: JsonObject?): Map<String, String> = event.headers.orEmpty().mapValues { (_, value) ->
        value.missingPlaceholders(data).firstOrNull()?.let { throw MissingValueException(it) }
        value.matchReplace(data)
    }
}

/** A request needs the value at [path], which the data doesn't have. */
class MissingValueException(val path: String) : IllegalArgumentException("This item has no $path")
