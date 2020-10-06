package it.sapienza.appinterpreter.utils.httpclient

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject


object HttpClient{
    val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
    private var client : OkHttpClient = let {
        val c = OkHttpClient().newBuilder()
        val logging = HttpLoggingInterceptor()
        logging.level = (HttpLoggingInterceptor.Level.BODY)
        c.addInterceptor(logging)
        c.build()
    }

    fun get(url: String, callback: Callback): Call {
        val request = Request.Builder()
            .url(url)
            .build()

        val call = client.newCall(request)
        call.enqueue(callback)
        return call
    }

    fun post(url: String, parameters: JSONObject?, callback: Callback): Call {
        val body: RequestBody = parameters.let { parameters.toString().toRequestBody(JSON) }
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val call = client.newCall(request)
        call.enqueue(callback)
        return call

    }
}
