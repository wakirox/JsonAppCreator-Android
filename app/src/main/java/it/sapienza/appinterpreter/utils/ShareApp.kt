package it.sapienza.appinterpreter.utils

import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.activity.EditorActivity
import java.io.File

/** Sends an app to someone as its JSON file, through the share sheet: chat apps, email, Quick Share, Drive… */
object ShareApp {
    fun share(context: Context, name: String, json: String) {
        val chooser = Intent.createChooser(intent(context, name, json), context.getString(R.string.share_app_title, name))
            // Appkin itself takes shared JSON (an AI app's answer), which makes no sense for its own app.
            .putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(ComponentName(context, EditorActivity::class.java)))
        context.startActivity(chooser)
    }

    /** The send intent, with the app written to a file in the cache that the receiving app may read. */
    fun intent(context: Context, name: String, json: String): Intent {
        // One folder per share, so the file keeps the app's name and an earlier share is never overwritten mid-send.
        val folder = File(File(context.cacheDir, FOLDER), System.nanoTime().toString()).apply { mkdirs() }
        val file = File(folder, SharedApps.fileName(name)).apply { writeText(json) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType(MIME_TYPE)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, name)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(name, uri)
        return send
    }

    private const val FOLDER = "shared"
    private const val MIME_TYPE = "application/json"
}
