package it.sapienza.appinterpreter.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Share links of chats in AI apps (ChatGPT, Gemini, Grok). Opening one reads that public page, as a browser would, to
 * find the AI's last configuration in it: nothing else is sent, and no AI runs in the app.
 *
 * The services don't document these pages, so reading them can stop working when they change; then the user is asked
 * to copy the answer instead.
 */
object ShareLinks {
    private val LINK = Regex(
        """https?://(?:www\.)?(?:chatgpt\.com/share/|chat\.openai\.com/share/|g\.co/gemini/share/|""" +
            """gemini\.google\.com/share/|share\.gemini\.google/|grok\.com/share/|x\.com/i/grok/share/)[\w\-./?=&%]+"""
    )
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36"

    private val client by lazy {
        OkHttpClient.Builder().followRedirects(true).callTimeout(30, TimeUnit.SECONDS).build()
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
    suspend fun configuration(link: String): String? = withContext(Dispatchers.IO) {
        val (page, finalUrl) = get(link)
        AiContext.lastConfiguration(page)?.let { return@withContext it }
        val id = finalUrl.substringAfter("/share/", "").substringBefore('?').substringBefore('/')
        when {
            // Gemini's page loads the chat afterwards, with this request of its own.
            "gemini.google.com" in finalUrl && id.isNotEmpty() -> geminiChat(id)?.let(AiContext::lastConfiguration)
            "grok.com" in finalUrl && id.isNotEmpty() ->
                get("https://grok.com/rest/app-chat/share_links/$id").first.let(AiContext::lastConfiguration)
            else -> null
        }
    }

    private fun get(url: String): Pair<String, String> {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        client.newCall(request).execute().use { response ->
            return (response.body?.string().orEmpty()) to response.request.url.toString()
        }
    }

    private fun geminiChat(id: String): String? {
        val body = FormBody.Builder()
            .add("f.req", """[[["ujx1Bf","[null,\"$id\"]",null,"generic"]]]""")
            .build()
        val request = Request.Builder()
            .url("https://gemini.google.com/_/BardChatUi/data/batchexecute?rpcids=ujx1Bf&source-path=%2Fshare%2F$id&hl=en&rt=c")
            .header("User-Agent", USER_AGENT)
            .post(body)
            .build()
        return client.newCall(request).execute().use { if (it.isSuccessful) it.body?.string() else null }
    }
}
