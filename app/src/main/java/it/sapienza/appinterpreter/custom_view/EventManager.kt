package it.sapienza.appinterpreter.custom_view

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import it.sapienza.appinterpreter.activity.MainActivity
import it.sapienza.appinterpreter.alerts.AlertUtils
import it.sapienza.appinterpreter.extensions.getObject
import it.sapienza.appinterpreter.extensions.matchReplace
import it.sapienza.appinterpreter.model.event.*
import it.sapienza.appinterpreter.utils.httpclient.HttpClient
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.function.Consumer
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject

object EventManager{
    fun manage(context: Context, event: RESTService, data: JSONObject?): Call? {
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
        showLoading: Boolean = true,
        failureConsumer: Consumer<String>? = null
    ): Call? {
        val url = event.url.matchReplace(data)

        val activity = (context as? MainActivity)

        fun reportFailure(message: String) {
            failureConsumer?.accept(message) ?: AlertUtils.showAlert(context, message)
        }

        if (showLoading) activity?.showLoading()

        val callback = object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (showLoading) activity?.hideLoading()
                response.use {
                    if (!response.isSuccessful) {
                        reportFailure("Request failed with ${response.code}")
                        return
                    }
                    try {
                        callbackConsumer.accept(JSONObject(response.body.string()))
                    } catch (error: Exception) {
                        reportFailure(error.message ?: "Unable to read the response")
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                if (showLoading) activity?.hideLoading()
                if (call.isCanceled()) return
                reportFailure(e.message ?: "An error occurred")
            }
        }
        return try {
            when(event.method){
                CallServiceMethod.post -> {
                    //TODO inserire l'oggetto
                    HttpClient.post(url, data, callback)
                }
                else -> {
                    HttpClient.get(url, callback)
                }
            }
        } catch (error: IllegalStateException) {
            if (showLoading) activity?.hideLoading()
            reportFailure(error.message ?: "A configuration API key is missing")
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
                    it.thenDoOK?.let { ev -> Runnable { evaluateEvent(context,ev,data) } },
                    it.thenDoKO?.let { ev -> Runnable { evaluateEvent(context,ev,data) } }
                )
            }
            is ShowView -> {
                val activity = context as MainActivity
                // The view receives the part of the data the mapping selects, such as "meals.0".
                val shown = it.mapping?.let { mapping -> data?.getObject(mapping) } ?: data
                activity.runOnUiThread { activity.pushScreen(it.view, shown) }
            }
            is RESTService -> {
                EventManager.manage(context, it, data)
            }
            is CalendarEvent -> openCalendar(context, it, data)
        }
    }

    /**
     * Opens the calendar app on a new event filled in from [event] and [data]. Nothing is written to the calendar
     * here: the user saves the event in their calendar app, so no calendar permission is needed.
     */
    private fun openCalendar(context: Context, event: CalendarEvent, data: JSONObject?) {
        val date = event.date.matchReplace(data)
        val time = event.time?.matchReplace(data)?.takeIf { it.isNotBlank() }
        val start = runCatching {
            SimpleDateFormat(if (time != null) "dd/MM/yyyy HH:mm" else "dd/MM/yyyy", Locale.ROOT)
                .apply { isLenient = false }
                .parse(if (time != null) "$date $time" else date)
        }.getOrNull()
        if (start == null) {
            AlertUtils.showAlert(context, "The event's date and time must be dd/MM/yyyy and HH:mm, but they are \"$date${time?.let { " $it" }.orEmpty()}\".")
            return
        }
        val end = if (time != null) start.time + (event.durationMinutes ?: 60) * 60_000L else start.time + 24 * 3_600_000L
        val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, event.title.matchReplace(data))
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start.time)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
            .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, time == null)
        event.location?.let { intent.putExtra(CalendarContract.Events.EVENT_LOCATION, it.matchReplace(data)) }
        event.description?.let { intent.putExtra(CalendarContract.Events.DESCRIPTION, it.matchReplace(data)) }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            AlertUtils.showAlert(context, "There is no calendar app on this phone to add the event to.")
        }
    }
}
