package it.sapienza.appinterpreter.runtime.usecase

import it.sapienza.appinterpreter.json.matchReplace
import it.sapienza.appinterpreter.json.missingPlaceholders
import it.sapienza.appinterpreter.model.event.CallServiceMethod
import it.sapienza.appinterpreter.model.event.RESTService
import it.sapienza.appinterpreter.utils.httpclient.JsonHttpClient
import kotlinx.serialization.json.JsonObject

/**
 * A request event's JSON answer: its URL filled in from the data, which a POST also sends as its body. A URL whose
 * placeholder has no value isn't requested: it fails with [MissingValueException] instead.
 */
class FetchJsonUseCase(private val client: JsonHttpClient) {
    suspend operator fun invoke(event: RESTService, data: JsonObject?): JsonObject {
        event.url.missingPlaceholders(data).firstOrNull()?.let { throw MissingValueException(it) }
        return client.request(event.url.matchReplace(data), post = event.method == CallServiceMethod.post, body = data)
    }
}

/** A request needs the value at [path], which the data doesn't have. */
class MissingValueException(val path: String) : IllegalArgumentException("This item has no $path")
