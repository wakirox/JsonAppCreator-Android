package it.sapienza.appinterpreter.alerts

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The alert a screen shows, if any, as state: the UI draws [alertState] (AlertHost on Android). */
class AlertsViewModel : ViewModel() {

    /** A message and what OK and Cancel do; without [onCancel] there is no Cancel button. */
    class Alert(val text: String, val onOk: (() -> Unit)?, val onCancel: (() -> Unit)?)

    private val current = MutableStateFlow<Alert?>(null)
    val alertState: StateFlow<Alert?> = current.asStateFlow()
    val alert: Alert? get() = current.value

    fun show(text: String, onOk: (() -> Unit)? = null, onCancel: (() -> Unit)? = null) {
        current.value = Alert(text, onOk, onCancel)
    }

    /** Closes the alert, then runs what OK ([ok]) or Cancel does. */
    fun answer(ok: Boolean) {
        val answered = current.value ?: return
        current.value = null
        (if (ok) answered.onOk else answered.onCancel)?.invoke()
    }

    /**
     * A recreated screen (after a theme or language change, say) keeps an alert that only informs, and closes one
     * whose actions belong to the screen before it.
     */
    fun activityRecreated() {
        if (current.value?.let { it.onOk != null || it.onCancel != null } == true) current.value = null
    }
}
