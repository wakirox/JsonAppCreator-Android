package it.sapienza.appinterpreter.utils.httpclient

import it.sapienza.appinterpreter.security.ApiKeyPlaceholders
import it.sapienza.appinterpreter.security.ConfigurationApiKeys
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject


object HttpClient{
    val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
    private var client: OkHttpClient = OkHttpClient.Builder().build()

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

    private fun withConfigurationApiKeys(url: String): String {
        val resolved = ConfigurationApiKeys.replaceForCurrentConfiguration(url)
        val missing = ApiKeyPlaceholders.names(resolved)
        check(missing.isEmpty()) {
            "Missing configuration API key: ${missing.joinToString()}"
        }
        return resolved
    }
}
