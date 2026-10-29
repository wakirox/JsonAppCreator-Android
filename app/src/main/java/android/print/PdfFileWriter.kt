package android.print

import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import java.io.File

/**
 * Runs a [PrintDocumentAdapter] (a WebView's) straight into a PDF [file], without the print screen. Its callbacks'
 * constructors aren't open to apps outside this package, hence the package; [onDone] gets false when the adapter
 * failed, so the caller can fall back to the print screen.
 */
class PdfFileWriter(private val attributes: PrintAttributes) {
    fun write(adapter: PrintDocumentAdapter, file: File, onDone: (Boolean) -> Unit) {
        adapter.onStart()
        val finish = { ok: Boolean ->
            adapter.onFinish()
            onDone(ok)
        }
        adapter.onLayout(null, attributes, CancellationSignal(), object : PrintDocumentAdapter.LayoutResultCallback() {
            override fun onLayoutFinished(info: PrintDocumentInfo, changed: Boolean) {
                val output = ParcelFileDescriptor.open(
                    file,
                    ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE or ParcelFileDescriptor.MODE_READ_WRITE
                )
                adapter.onWrite(arrayOf(PageRange.ALL_PAGES), output, CancellationSignal(), object : PrintDocumentAdapter.WriteResultCallback() {
                    override fun onWriteFinished(pages: Array<out PageRange>) {
                        output.close()
                        finish(file.length() > 0)
                    }

                    override fun onWriteFailed(error: CharSequence?) {
                        output.close()
                        finish(false)
                    }

                    override fun onWriteCancelled() {
                        output.close()
                        finish(false)
                    }
                })
            }

            override fun onLayoutFailed(error: CharSequence?) = finish(false)

            override fun onLayoutCancelled() = finish(false)
        }, null)
    }
}
