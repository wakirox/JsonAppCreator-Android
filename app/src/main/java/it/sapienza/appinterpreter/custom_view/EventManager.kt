package it.sapienza.appinterpreter.custom_view

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat.startActivity
import androidx.core.util.Consumer
import it.sapienza.appinterpreter.activity.MainActivity
import it.sapienza.appinterpreter.alerts.AlertUtils
import it.sapienza.appinterpreter.extensions.matchReplace
import it.sapienza.appinterpreter.model.event.*
import it.sapienza.appinterpreter.utils.DomainController
import it.sapienza.appinterpreter.utils.httpclient.HttpClient
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException


object EventManager {
    fun manage(context: Context, event: RESTService, data: JSONObject?) {
        manage(context, event, data, Consumer { response ->
            event.thenDo?.let {
                evaluateEvent(context, it, response)
            }
        })
    }

    fun manage(
        context: Context,
        event: RESTService,
        data: JSONObject?,
        callbackConsumer: Consumer<JSONObject>
    ) {
        val url = event.url.matchReplace(data)

        val activity = (context as? MainActivity)

        activity?.showLoading()

        val callback = object : Callback {
            override fun onResponse(call: Call, response: Response) {
                activity?.hideLoading()
                if (response.code != 200) {
                    onFailure(
                        call,
                        IOException("Some error occured with ${call.request().url}\n${response.body?.string()}")
                    )
                } else {
                    response.body?.let { d ->
                        callbackConsumer.accept(JSONObject(d.string()))
                    } ?: AlertUtils.showAlert(context, "No body in response")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                activity?.hideLoading()
                AlertUtils.showAlert(context, e.message ?: "An error occurred")
            }
        }
        when (event.method) {
            CallServiceMethod.post -> {
                HttpClient.post(HttpClient.workUrl(url, context), data, callback)
            }
            else -> {
                HttpClient.get(HttpClient.workUrl(url, context), callback)
            }
        }
    }

    fun getInitDataSyncronous(
        context: Context,
        event: DataService,
        data: JSONObject?
    ) : JSONObject? {

        if(event is RESTService){
            return manageSyncronous(context, event, data)
        }else if(event is GetSavedDataService){
            return DomainController.getJson(context, event.key)
        }
        return null
    }

    fun manageSyncronous(
        context: Context,
        event: RESTService,
        data: JSONObject?,
        page: Int? = null
    ): JSONObject? {
        var url = event.url.matchReplace(data)

        event.paginated?.let { paginatedData ->
            url = HttpClient.addPage(url, paginatedData, page ?: 0)
        }

        val activity = (context as? MainActivity)

        activity?.showLoading()

        val response: Response = when (event.method) {
            CallServiceMethod.post -> {
                HttpClient.postSyncronous(HttpClient.workUrl(url, context), data)

            }
            else -> {
                HttpClient.getSyncronous(HttpClient.workUrl(url, context))
            }
        }

        activity?.hideLoading()

        return if (response.isSuccessful) {
            response.body?.let { d ->
                JSONObject(d.string())
            } ?: kotlin.run {
                AlertUtils.showAlert(context, "No body in response")
                null
            }
        } else {
            AlertUtils.showAlert(
                context,
                "Some error occured with ${response.request.url}\n${response.body?.string()}"
            )
            null
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
                    it.thenDoOK?.let { ev -> Runnable { evaluateEvent(context, ev, data) } },
                    it.thenDoKO?.let { ev -> Runnable { evaluateEvent(context, ev, data) } }
                )
            }
            is ShowView -> {
                (context as MainActivity).pushScreen(it.view, data)
            }
            is OpenSiteService -> {
                openSite(context, it, data)
            }
            is RESTService -> {
                manage(context, it, data)
            }
            is SaveDataService -> {
                (data ?: it.dataObj)?.let { innerData ->
                    DomainController.saveJson(context, it.key, innerData)
                    it.thenDo?.let { ev ->
                        evaluateEvent(context, ev, innerData)
                    }
                } ?: kotlin.run {
                    it.thenDo?.let { ev ->
                        evaluateEvent(context, ev, data)
                    }
                }
            }
            is GetSavedDataService -> {
                val json = DomainController.getJson(context, it.key)
                it.thenDo?.let { ev ->
                    evaluateEvent(
                        context,
                        ev,
                        json ?: it.dataObj ?: data
                    )
                }
            }
        }
    }

    private fun openSite(context: Context, event: OpenSiteService, data: JSONObject?) {
        val url = event.url.matchReplace(data)
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(browserIntent)
    }
}

