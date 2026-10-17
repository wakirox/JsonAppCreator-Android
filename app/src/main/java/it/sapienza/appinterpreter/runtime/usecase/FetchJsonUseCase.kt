package it.sapienza.appinterpreter.runtime.usecase

import it.sapienza.appinterpreter.extensions.matchReplace
import it.sapienza.appinterpreter.model.event.CallServiceMethod
import it.sapienza.appinterpreter.model.event.RESTService
import it.sapienza.appinterpreter.utils.httpclient.HttpClient
import org.json.JSONObject

/** A request event's JSON answer: its URL filled in from the data, which a POST also sends as its body. */
class FetchJsonUseCase {
    suspend operator fun invoke(event: RESTService, data: JSONObject?): JSONObject =
        HttpClient.request(event.url.matchReplace(data), post = event.method == CallServiceMethod.post, body = data)
}
