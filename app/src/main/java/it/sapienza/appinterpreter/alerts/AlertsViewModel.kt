package it.sapienza.appinterpreter.alerts

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel

/** The alert an activity shows, if any, as state: [AlertHost] draws it in the activity's composition. */
class AlertsViewModel : ViewModel() {

    /** A message and what OK and Cancel do; without [onCancel] there is no Cancel button. */
    class Alert(val text: String, val onOk: Runnable?, val onCancel: Runnable?)

    var alert by mutableStateOf<Alert?>(null)
        private set

    fun show(text: String, onOk: Runnable? = null, onCancel: Runnable? = null) {
        alert = Alert(text, onOk, onCancel)
    }

    /** Closes the alert, then runs what OK ([ok]) or Cancel does. */
    fun answer(ok: Boolean) {
        val answered = alert ?: return
        alert = null
        (if (ok) answered.onOk else answered.onCancel)?.run()
    }

    /**
     * A recreated activity (after a theme or language change, say) keeps an alert that only informs, and closes one
     * whose actions belong to the activity before it.
     */
    fun activityRecreated() {
        if (alert?.let { it.onOk != null || it.onCancel != null } == true) alert = null
    }
}

/** [viewModel]'s alert as a Material 3 dialog, which closes only through its buttons. */
@Composable
fun AlertHost(viewModel: AlertsViewModel) {
    val alert = viewModel.alert ?: return
    AlertDialog(
        onDismissRequest = {},
        text = { Text(alert.text) },
        confirmButton = {
            TextButton(onClick = { viewModel.answer(ok = true) }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = alert.onCancel?.let {
            { TextButton(onClick = { viewModel.answer(ok = false) }) { Text(stringResource(android.R.string.cancel)) } }
        }
    )
}
