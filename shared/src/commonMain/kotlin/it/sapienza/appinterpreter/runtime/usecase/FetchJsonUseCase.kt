package it.sapienza.appinterpreter.runtime.usecase

import it.sapienza.appinterpreter.json.matchReplace
import it.sapienza.appinterpreter.model.event.CallServiceMethod
import it.sapienza.appinterpreter.model.event.RESTService
import it.sapienza.appinterpreter.utils.httpclient.JsonHttpClient
import kotlinx.serialization.json.JsonObject

/** A request event's JSON answer: its URL filled in from the data, which a POST also sends as its body. */
class FetchJsonUseCase(private val client: JsonHttpClient) {
    suspend operator fun invoke(event: RESTService, data: JsonObject?): JsonObject =
        client.request(event.url.matchReplace(data), post = event.method == CallServiceMethod.post, body = data)
}
