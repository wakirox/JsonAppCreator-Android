package it.sapienza.appinterpreter.custom_view

import android.content.Context
import it.sapienza.appinterpreter.activity.MainActivity
import it.sapienza.appinterpreter.alerts.AlertUtils
import it.sapienza.appinterpreter.extensions.matchReplace
import it.sapienza.appinterpreter.model.event.*
import it.sapienza.appinterpreter.utils.httpclient.HttpClient
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.function.Consumer

object EventManager{
    fun manage(context: Context, event: RESTService, data: JSONObject?): Call {
        return manage(context,event,data, Consumer { response ->
            event.thenDo?.let {
                evaluateEvent(context,it,response)
            }
        })
    }

    fun manage(
        context: Context,
        event: RESTService,
        data: JSONObject?,
        callbackConsumer: Consumer<JSONObject>,
        showLoading: Boolean = true
    ): Call {
        val url = event.url.matchReplace(data)

        val activity = (context as? MainActivity)

        if (showLoading) activity?.showLoading()

        val callback = object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (showLoading) activity?.hideLoading()
                response.use {
                    if (!response.isSuccessful) {
                        val details = response.body?.string().orEmpty()
                        AlertUtils.showAlert(
                            context,
                            "Request failed with ${response.code} for ${call.request().url}\n$details"
                        )
                        return
                    }
                    response.body?.let { body ->
                        callbackConsumer.accept(JSONObject(body.string()))
                    } ?: AlertUtils.showAlert(context, "No body in response")
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                if (showLoading) activity?.hideLoading()
                if (call.isCanceled()) return
                AlertUtils.showAlert(context,e.message ?: "An error occurred")
            }
        }
        return when(event.method){
            CallServiceMethod.post -> {
                //TODO inserire l'oggetto
                HttpClient.post(url, data, callback)
            }
            else -> {
                HttpClient.get(url, callback)
            }
        }
    }

    fun evaluateEvent(
        context: Context,
        it: Event,
        data: JSONObject?
    ) {
        when (it) {
            is AlertMessage -> {
                AlertUtils.showAlert(context, it, data,
                    it.thenDoOK?.let { ev -> Runnable { evaluateEvent(context,ev,data) } },
                    it.thenDoKO?.let { ev -> Runnable { evaluateEvent(context,ev,data) } }
                )
            }
            is ShowView -> {
                val activity = context as MainActivity
                activity.runOnUiThread { activity.pushScreen(it.view, data) }
            }
            is RESTService -> {
                EventManager.manage(context, it, data)
            }
        }
    }
}
