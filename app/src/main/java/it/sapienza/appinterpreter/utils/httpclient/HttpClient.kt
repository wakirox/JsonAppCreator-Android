package it.sapienza.appinterpreter.utils.httpclient

import it.sapienza.androidratio.appratio.BuildConfig
import it.sapienza.appinterpreter.security.ApiKeyPlaceholders
import it.sapienza.appinterpreter.security.ConfigurationApiKeys
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


object HttpClient{
    val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()

    /**
     * The one OkHttp client for the app's requests and its images (Coil uses it too). It names the app in every
     * request's User-Agent, as sites such as Wikimedia require: they refuse OkHttp's default one.
     */
    val client: OkHttpClient = OkHttpClient.Builder()
        .addNetworkInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
        }
        .build()

    private val USER_AGENT =
        "Appkin/${BuildConfig.VERSION_NAME} (Android; https://github.com/wakirox/JsonAppCreator-Android)"

    fun get(url: String, callback: Callback): Call {
        val request = Request.Builder()
            .url(withConfigurationApiKeys(url))
            .build()

        val call = client.newCall(request)
        call.enqueue(callback)
        return call
    }

    fun post(url: String, parameters: JSONObject?, callback: Callback): Call {
        val body: RequestBody = parameters.let { parameters.toString().toRequestBody(JSON) }
        val request = Request.Builder()
            .url(withConfigurationApiKeys(url))
            .post(body)
            .build()

        val call = client.newCall(request)
        call.enqueue(callback)
        return call

    }

    /**
     * Requests [url], by POST with [body] when [post], and returns the JSON it answers. Cancelling the coroutine
     * cancels the request. A failure is an exception whose message says what went wrong, such as a missing API key.
     */
    suspend fun request(url: String, post: Boolean, body: JSONObject?): JSONObject = suspendCancellableCoroutine { continuation ->
        val callback = object : Callback {
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        continuation.resumeWithException(IOException("Request failed with ${response.code}"))
                        return
                    }
                    runCatching { JSONObject(response.body.string()) }
                        .onSuccess(continuation::resume)
                        .onFailure { continuation.resumeWithException(IOException(it.message ?: "Unable to read the response")) }
                }
            }

            override fun onFailure(call: Call, e: IOException) = continuation.resumeWithException(e)
        }
        val call = if (post) post(url, body, callback) else get(url, callback)
        continuation.invokeOnCancellation { call.cancel() }
    }

    private fun withConfigurationApiKeys(url: String): String {
        val resolved = ConfigurationApiKeys.replaceForCurrentConfiguration(url)
        val missing = ApiKeyPlaceholders.names(resolved)
        check(missing.isEmpty()) {
            "Missing configuration API key: ${missing.joinToString()}"
        }
        return resolved
    }
}
