package it.sapienza.appinterpreter.utils

import android.app.Activity
import android.content.Intent
import android.print.PdfFileWriter
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
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
            html =
                html.replace(
                    "\"$reference\"",
                    "\"data:image/jpeg;base64,${Base64.encodeToString(jpeg, Base64.NO_WRAP)}\""
                )
        }
        val view = WebView(activity)
        printing = view
        // Only to measure a pageless page: the page itself has no scripts, and saved text in it is escaped.
        view.settings.javaScriptEnabled = page.pageless
        view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                if (!page.pageless) return write(view, page, A4)
                view.evaluateJavascript(MEASURE) { height ->
                    val attributes = height.toFloatOrNull()?.let(::pageless) ?: A4
                    write(view, page, attributes)
                }
            }
        }
        view.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    private fun write(view: WebView, page: PdfPageUseCase.Page, attributes: PrintAttributes) {
        val name = fileName(page.title)
        val folder = File(activity.cacheDir, "shared/pdf").apply {
            deleteRecursively()
            mkdirs()
        }
        val file = File(folder, "$name.pdf")
        PdfFileWriter(attributes).write(view.createPrintDocumentAdapter(name), file) { written ->
            if (written) activity.startActivity(shareIntent(file, name)) else print(view, name, attributes)
            printing = null
        }
    }

    private fun print(view: WebView, name: String, attributes: PrintAttributes) {
        activity.getSystemService(
            PrintManager::class.java
        ).print(name, view.createPrintDocumentAdapter(name), attributes)
    }

    /** The share sheet on the PDF [file], readable by the app the user picks. */
    internal fun shareIntent(file: File, name: String): Intent =
        ExternalApps.shareFile(activity, file, "application/pdf", name, shareTitle)

    companion object {
        private val A4_MEDIA = PrintAttributes.MediaSize.ISO_A4
        private val RESOLUTION = PrintAttributes.Resolution("pdf", "pdf", 600, 600)
        private val A4 = PrintAttributes.Builder()
            .setMediaSize(A4_MEDIA)
            .setResolution(RESOLUTION)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        /** The height of the page's content, in CSS pixels (96 to the inch). */
        private const val MEASURE = "document.querySelector('main').getBoundingClientRect().height"

        /** The tallest page a PDF may have: 200 inches. A taller page goes on over a second one. */
        private const val MAX_PAGE_MILS = 200_000
        private const val MIN_PAGE_MILS = 2_000

        /**
         * One A4-wide page as tall as [contentHeight] CSS pixels, plus the margins above and below and a little room,
         * so rounding can't push the last line onto a second page.
         */
        fun pageless(contentHeight: Float): PrintAttributes {
            val heightMm = contentHeight * MM_PER_INCH / CSS_PIXELS_PER_INCH + 2 * PdfPageUseCase.MARGIN_MM + SLACK_MM
            val heightMils = (heightMm / MM_PER_INCH * 1000).toInt().coerceIn(MIN_PAGE_MILS, MAX_PAGE_MILS)
            return PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize("pageless", "Pageless", A4_MEDIA.widthMils, heightMils))
                .setResolution(RESOLUTION)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()
        }

        private const val MM_PER_INCH = 25.4f
        private const val CSS_PIXELS_PER_INCH = 96f
        private const val SLACK_MM = 4

        /** A title as a file name: without the characters file systems refuse ("Rainy: ideas" is "Rainy ideas"). */
        fun fileName(title: String): String =
            title.replace(Regex("[\\\\/:*?\"<>|]"), " ").replace(Regex("\\s+"), " ").trim().ifEmpty { "Appkin" }
    }
}
