package it.sapienza.appinterpreter.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Share links of chats in AI apps (ChatGPT, Gemini, Grok). Opening one reads that public page, as a browser would, to
 * find the AI's last configuration in it: nothing else is sent, and no AI runs in the app.
 *
 * The services don't document these pages, so reading them can stop working when they change; then the user is asked
 * to copy the answer instead.
 */
object ShareLinks {
    private val LINK = Regex(
        """https?://(?:www\.)?(?:chatgpt\.com/share/|chatgpt\.com/s/|chat\.openai\.com/share/|g\.co/gemini/share/|""" +
            """gemini\.google\.com/share/|share\.gemini\.google/|grok\.com/share/|x\.com/i/grok/share/)[\w\-./?=&%]+"""
    )
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36"

    /** The client share pages are read with: redirects followed, 30 seconds at most per request. */
    fun client(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
        expectSuccess = false
        install(HttpTimeout) { requestTimeoutMillis = 30_000 }
    }

    /** The first chat share link in [text], if it has one. */
    fun find(text: String): String? = LINK.find(text)?.value

    /** "ChatGPT", "Gemini" or "Grok", for messages. */
    fun service(link: String): String = when {
        "gemini" in link -> "Gemini"
        "grok" in link -> "Grok"
        else -> "ChatGPT"
    }

    /** The AI's last configuration in the shared chat, or null if the page doesn't show one. */
    suspend fun configuration(client: HttpClient, link: String, promptExamples: Collection<String>): String? {
        val (page, finalUrl) = get(client, link)
        lastConfiguration(page, promptExamples)?.let { return it }
        val id = finalUrl.substringAfter("/share/", "").substringBefore('?').substringBefore('/')
        return when {
            // Gemini's page loads the chat afterwards, with this request of its own.
            "gemini.google.com" in finalUrl && id.isNotEmpty() -> geminiChat(client, id)?.let {
                lastConfiguration(it, promptExamples)
            }

            "grok.com" in finalUrl && id.isNotEmpty() ->
                get(client, "https://grok.com/rest/app-chat/share_links/$id").first.let {
                    lastConfiguration(it, promptExamples)
                }

            else -> null
        }
    }

    /** Pages are large: they are searched off the main thread. */
    private suspend fun lastConfiguration(page: String, promptExamples: Collection<String>) =
        withContext(Dispatchers.Default) { AiContext.lastConfiguration(page, promptExamples) }

    private suspend fun get(client: HttpClient, url: String): Pair<String, String> {
        val response = client.get(url) { header(HttpHeaders.UserAgent, USER_AGENT) }
        return response.bodyAsText() to response.call.request.url.toString()
    }

    private suspend fun geminiChat(client: HttpClient, id: String): String? {
        val response = client.submitForm(
            "https://gemini.google.com/_/BardChatUi/data/batchexecute?rpcids=ujx1Bf&source-path=%2Fshare%2F$id&hl=en&rt=c",
            parameters { append("f.req", """[[["ujx1Bf","[null,\"$id\"]",null,"generic"]]]""") }
        ) { header(HttpHeaders.UserAgent, USER_AGENT) }
        return if (response.status.isSuccess()) response.bodyAsText() else null
    }
}
