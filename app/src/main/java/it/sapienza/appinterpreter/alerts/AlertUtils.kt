package it.sapienza.appinterpreter.alerts

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner


object AlertUtils {

    /**
     * Shows [text] with OK, and Cancel when [ko] is given, through the activity's [AlertsViewModel]: the activity
     * draws it with [AlertHost]. Callable from any thread, such as a network callback.
     */
    fun showAlert(context: Context, text: String, ok: Runnable? = null, ko: Runnable? = null) {
        val activity = context as Activity
        activity.runOnUiThread { alertsOf(activity).show(text, ok?.let { { it.run() } }, ko?.let { { it.run() } }) }
    }

    /** The [AlertsViewModel] of [activity], which hosts it. */
    fun alertsOf(activity: Activity): AlertsViewModel =
        ViewModelProvider(activity as ViewModelStoreOwner)[AlertsViewModel::class.java]
}
