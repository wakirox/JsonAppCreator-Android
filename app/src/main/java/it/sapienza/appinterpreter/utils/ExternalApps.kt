package it.sapienza.appinterpreter.utils

import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import it.sapienza.appinterpreter.activity.EditorActivity
import java.io.File

/** The intents of a JSON app's open and share events: other apps on the phone do the rest. */
object ExternalApps {

    /**
     * [url] for the app that handles its [scheme] (see OpenLinkUseCase): tel: only fills in the dialer, so Appkin
     * needs no call permission, and mailto: goes to email apps alone.
     */
    fun open(url: String, scheme: String): Intent {
        val uri = url.toUri()
        return when (scheme) {
            "tel" -> Intent(Intent.ACTION_DIAL, uri)
            "mailto" -> Intent(Intent.ACTION_SENDTO, uri)
            else -> Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
        }
    }

    /** The share sheet on [text], without Appkin's own editor, which would take it as an app's JSON. */
    fun share(context: Context, text: String, title: String, photos: List<Uri> = emptyList()): Intent {
        val send = when (photos.size) {
            0 -> Intent(Intent.ACTION_SEND).setType("text/plain")
            1 -> Intent(Intent.ACTION_SEND).setType("image/jpeg").putExtra(Intent.EXTRA_STREAM, photos.single())
            else -> Intent(Intent.ACTION_SEND_MULTIPLE).setType("image/jpeg")
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(photos))
        }
        if (text.isNotBlank()) send.putExtra(Intent.EXTRA_TEXT, text)
        // The receiving app may read the photos: the grant travels with the clip data, also through the chooser.
        val clip = photos.takeIf { it.isNotEmpty() }?.let { uris ->
            ClipData.newRawUri(null, uris.first()).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
        }
        clip?.let { send.clipData = it; send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        return Intent.createChooser(send, title)
            .putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(ComponentName(context, EditorActivity::class.java)))
            .apply { clip?.let { clipData = it; addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
    }

    /** The share sheet on [file] (in the shared cache) as [mimeType], named [name], readable by the app picked. */
    fun shareFile(context: Context, file: File, mimeType: String, name: String, title: String): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND).setType(mimeType)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TITLE, name)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(name, uri)
        return Intent.createChooser(send, title)
            .putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(ComponentName(context, EditorActivity::class.java)))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .apply { clipData = send.clipData }
    }
}
