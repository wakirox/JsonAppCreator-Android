package it.sapienza.appinterpreter.runtime.usecase

import it.sapienza.appinterpreter.json.fillAddress
import it.sapienza.appinterpreter.json.missingAddressPlaceholders
import it.sapienza.appinterpreter.model.event.OpenLink
import kotlinx.serialization.json.JsonObject

/** The address an open event hands to the phone, filled in from the data, if it may be opened. */
class OpenLinkUseCase {

    sealed interface Result {
        /** Open [url]; its [scheme] (lower case) says what kind of app opens it. */
        class Ready(val url: String, val scheme: String) : Result
        /** The data has no value at [path], which the address needs. */
        class Missing(val path: String) : Result
        /** [url] isn't a web, map, phone or email address, so it isn't opened. */
        class NotAllowed(val url: String) : Result
    }

    operator fun invoke(event: OpenLink, data: JsonObject?): Result {
        val url = event.url.fillAddress(data)?.trim()
            ?: return Result.Missing(event.url.missingAddressPlaceholders(data).first())
        val scheme = url.substringBefore(':', missingDelimiterValue = "").lowercase()
        if (scheme !in SCHEMES || url.length == scheme.length + 1) return Result.NotAllowed(url)
        return Result.Ready(url, scheme)
    }

    companion object {
        /** What an app may open: web pages, maps, the dialer and email. */
        val SCHEMES = setOf("https", "http", "geo", "tel", "mailto")
    }
}
