package it.sapienza.appinterpreter.utils

import android.app.Activity
import android.content.ClipData
import android.content.ComponentName
import android.content.Intent
import android.print.PdfFileWriter
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import it.sapienza.appinterpreter.activity.EditorActivity
import it.sapienza.appinterpreter.runtime.usecase.PdfPageUseCase
import java.io.File

/**
 * Makes a PDF of a [PdfPageUseCase.Page] and opens the share sheet on it. The page is laid out by a WebView no one
 * sees; kept photos go in as JPEG data, so the page needs no file access. Should the WebView not write the file, the
 * phone's print screen opens on it instead, where "Save as PDF" makes it.
 */
class PdfPrint(private val activity: Activity, private val shareTitle: String) {
    /** The WebView being printed: it must live until the PDF is written. */
    private var printing: WebView? = null

    suspend fun share(page: PdfPageUseCase.Page) {
        val reader = PhotoStore(activity).reader
        var html = page.html
        page.photos.forEach { reference ->
            val jpeg = reader.jpeg(reference) ?: return@forEach
            html = html.replace("\"$reference\"", "\"data:image/jpeg;base64,${Base64.encodeToString(jpeg, Base64.NO_WRAP)}\"")
        }
        val view = WebView(activity)
        printing = view
        view.settings.javaScriptEnabled = false
        view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val name = fileName(page.title)
                val folder = File(activity.cacheDir, "shared/pdf").apply { deleteRecursively(); mkdirs() }
                val file = File(folder, "$name.pdf")
                PdfFileWriter(A4).write(view.createPrintDocumentAdapter(name), file) { written ->
                    if (written) activity.startActivity(shareIntent(file, name)) else print(view, name)
                    printing = null
                }
            }
        }
        view.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    private fun print(view: WebView, name: String) {
        activity.getSystemService(PrintManager::class.java).print(name, view.createPrintDocumentAdapter(name), A4)
    }

    /** The share sheet on the PDF [file], readable by the app the user picks. */
    internal fun shareIntent(file: File, name: String): Intent {
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND).setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TITLE, name)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(name, uri)
        return Intent.createChooser(send, shareTitle)
            .putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(ComponentName(activity, EditorActivity::class.java)))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .apply { clipData = send.clipData }
    }

    companion object {
        private val A4 = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        /** A title as a file name: without the characters file systems refuse ("Rainy: ideas" is "Rainy ideas"). */
        fun fileName(title: String): String =
            title.replace(Regex("[\\\\/:*?\"<>|]"), " ").replace(Regex("\\s+"), " ").trim().ifEmpty { "Appkin" }
    }
}
