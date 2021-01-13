package it.sapienza.appinterpreter.utils.httpclient

import android.content.Context
import it.sapienza.appinterpreter.AppRatioApplication
import it.sapienza.appinterpreter.model.event.PaginatedData
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
        logging.level = HttpLoggingInterceptor.Level.BODY
//        logging.level = HttpLoggingInterceptor.Level.BODY
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

    fun getSyncronous(url: String): Response {
        val request = Request.Builder()
            .url(url)
            .build()
        return  client.newCall(request).execute()
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

    fun postSyncronous(url: String, parameters: JSONObject?): Response {
        val body: RequestBody = parameters.let { parameters.toString().toRequestBody(JSON) }
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return client.newCall(request).execute()
    }

    fun addPage(url : String, paginatedData: PaginatedData, page : Int = 0) : String {
        return if(url.contains("?")){
            "$url&${paginatedData.pageMapping}=${page+1}"
        }else{
            "$url?${paginatedData.pageMapping}=${page+1}"
        }
    }

    fun workUrl(url : String, context : Context) : String {
        return (context.applicationContext as AppRatioApplication).app?.params?.let {
            if(it.apiToken == null || it.apiTokenName == null || url.contains(it.apiToken)){
                url
            }else if(url.contains("?")){
                "$url&${it.apiTokenName}=${it.apiToken}"
            }else{
                "$url?${it.apiTokenName}=${it.apiToken}"
            }
        } ?: url

    }
}
