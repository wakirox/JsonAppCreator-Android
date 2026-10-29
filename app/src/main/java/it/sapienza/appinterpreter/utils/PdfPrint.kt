package it.sapienza.appinterpreter.utils

import android.app.Activity
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import it.sapienza.appinterpreter.runtime.usecase.PdfPageUseCase

/**
 * Prints a [PdfPageUseCase.Page] through the phone's print screen, where "Save as PDF" makes the file. The page is
 * laid out by a WebView no one sees; kept photos go in as JPEG data, so the page needs no file access.
 */
class PdfPrint(private val activity: Activity) {
    /** The WebView being printed: it must live until the print screen has read it. */
    private var printing: WebView? = null

    suspend fun print(page: PdfPageUseCase.Page) {
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
                activity.getSystemService(PrintManager::class.java).print(
                    name,
                    view.createPrintDocumentAdapter(name),
                    PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build()
                )
            }
        }
        view.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    companion object {
        /** A title as a file name: without the characters file systems refuse. */
        fun fileName(title: String): String = title.replace(Regex("[\\\\/:*?\"<>|]"), " ").trim().ifEmpty { "Appkin" }
    }
}
