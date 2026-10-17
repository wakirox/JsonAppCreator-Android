package it.sapienza.appinterpreter.alerts

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import it.sapienza.appinterpreter.extensions.getValue
import it.sapienza.appinterpreter.extensions.matchReplace
import it.sapienza.appinterpreter.model.event.AlertMessage
import org.json.JSONObject


object AlertUtils {

    /**
     * Shows [text] with OK, and Cancel when [ko] is given, through the activity's [AlertsViewModel]: the activity
     * draws it with [AlertHost]. Callable from any thread, such as a network callback.
     */
    fun showAlert(context: Context, text: String, ok: Runnable? = null, ko: Runnable? = null) {
        val activity = context as Activity
        activity.runOnUiThread { alertsOf(activity).show(text, ok, ko) }
    }

    /** The [AlertsViewModel] of [activity], which hosts it. */
    fun alertsOf(activity: Activity): AlertsViewModel =
        ViewModelProvider(activity as ViewModelStoreOwner)[AlertsViewModel::class.java]

    fun showAlert(context: Context, event: AlertMessage, data: JSONObject?, ok : Runnable? = null, ko : Runnable? = null) {
        val message = event.mapping?.let {
            data?.getValue(it)
        } ?: event.value
        // An alert that does something on OK can always be declined: Cancel then just closes it.
        val decline = ko ?: if (ok != null) Runnable { } else null
        showAlert(context, message?.matchReplace(data) ?: "Missing message (check model)", ok, decline)
    }
}
