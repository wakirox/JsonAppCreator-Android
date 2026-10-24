package it.sapienza.appinterpreter.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import it.sapienza.appinterpreter.activity.EditorActivity

/** The intents of a JSON app's open and share events: other apps on the phone do the rest. */
object ExternalApps {

    /**
     * [url] for the app that handles its [scheme] (see OpenLinkUseCase): tel: only fills in the dialer, so Appkin
     * needs no call permission, and mailto: goes to email apps alone.
     */
    fun open(url: String, scheme: String): Intent {
        val uri = Uri.parse(url)
        return when (scheme) {
            "tel" -> Intent(Intent.ACTION_DIAL, uri)
            "mailto" -> Intent(Intent.ACTION_SENDTO, uri)
            else -> Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
        }
    }

    /** The share sheet on [text], without Appkin's own editor, which would take it as an app's JSON. */
    fun share(context: Context, text: String, title: String): Intent =
        Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), title)
            .putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(ComponentName(context, EditorActivity::class.java)))
}
