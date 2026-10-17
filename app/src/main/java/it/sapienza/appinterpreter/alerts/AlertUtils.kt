package it.sapienza.appinterpreter.alerts

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import it.sapienza.appinterpreter.extensions.getValue
import it.sapienza.appinterpreter.extensions.matchReplace
import it.sapienza.appinterpreter.model.event.AlertMessage
import it.sapienza.appinterpreter.ui.AppTheme
import org.json.JSONObject


object AlertUtils {

    /**
     * A Material 3 dialog in the app's theme, over whatever the activity shows: [text], OK, and Cancel when [ko] is
     * given. It closes only through its buttons.
     */
    fun showAlert(context: Context, text: String, ok: Runnable? = null, ko: Runnable? = null) {
        val activity = context as Activity
        activity.runOnUiThread {
            val root = activity.findViewById<ViewGroup>(android.R.id.content)
            val host = ComposeView(activity)
            fun close(then: Runnable?) {
                root.removeView(host)
                then?.run()
            }
            host.setContent {
                AppTheme(surface = false) {
                    AlertDialog(
                        onDismissRequest = {},
                        text = { Text(text) },
                        confirmButton = { TextButton(onClick = { close(ok) }) { Text(stringResource(android.R.string.ok)) } },
                        dismissButton = ko?.let {
                            { TextButton(onClick = { close(it) }) { Text(stringResource(android.R.string.cancel)) } }
                        }
                    )
                }
            }
            root.addView(host)
        }
    }

    fun showAlert(context: Context, event: AlertMessage, data: JSONObject?, ok : Runnable? = null, ko : Runnable? = null) {
        val message = event.mapping?.let {
            data?.getValue(it)
        } ?: event.value
        // An alert that does something on OK can always be declined: Cancel then just closes it.
        val decline = ko ?: if (ok != null) Runnable { } else null
        showAlert(context, message?.matchReplace(data) ?: "Missing message (check model)", ok, decline)
    }
}
