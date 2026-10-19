package it.sapienza.appinterpreter.utils.httpclient

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.UserAgent
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import it.sapienza.appinterpreter.json.JsonText
import it.sapienza.appinterpreter.security.ApiKeyPlaceholders
import kotlinx.io.IOException
import kotlinx.serialization.json.JsonObject

/**
 * The requests of a JSON app's REST events and lists, on Ktor. [userAgent] names the app in every request, as sites
 * such as Wikimedia require (they refuse a library's default one); [resolveUrl] puts the configuration's API keys in
 * the URL.
 */
class JsonHttpClient(
    engine: HttpClientEngine,
    userAgent: String,
    private val resolveUrl: (String) -> String = { it }
) {
    private val client = HttpClient(engine) {
        expectSuccess = false
        install(UserAgent) { agent = userAgent }
        // Like OkHttp did on its own: follow redirects for POST too.
        install(HttpRedirect) { checkHttpMethod = false }
    }

    /**
     * Requests [url], by POST with [body] when [post], and returns the JSON object it answers. Cancelling the coroutine
     * cancels the request. A failure is an exception whose message says what went wrong, such as a missing API key.
     */
    suspend fun request(url: String, post: Boolean, body: JsonObject?): JsonObject {
        val resolved = resolveUrl(url)
        val missing = ApiKeyPlaceholders.names(resolved)
        check(missing.isEmpty()) { "Missing configuration API key: ${missing.joinToString()}" }
        val response = client.request(resolved) {
            method = if (post) HttpMethod.Post else HttpMethod.Get
            // As before, a POST without data sends the text null.
            if (post) setBody(TextContent(body.toString(), JSON))
        }
        if (!response.status.isSuccess()) throw IOException("Request failed with ${response.status.value}")
        val text = response.bodyAsText()
        return try {
            JsonText.parse(text) as? JsonObject ?: throw IOException("The response is not a JSON object")
        } catch (error: IllegalArgumentException) {
            throw IOException(error.message ?: "Unable to read the response")
        }
    }

    fun close() = client.close()

    companion object {
        val JSON = ContentType.parse("application/json; charset=utf-8")

        /** "Appkin/1.2 (Android; https://...)": the app, its version and platform, and where it comes from. */
        fun userAgent(versionName: String, platform: String) =
            "Appkin/$versionName ($platform; https://github.com/wakirox/JsonAppCreator-Android)"
    }
}
