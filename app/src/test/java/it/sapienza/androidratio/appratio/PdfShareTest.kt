package it.sapienza.androidratio.appratio

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import it.sapienza.appinterpreter.utils.PdfPrint
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A PDF goes straight to the share sheet, as a file the picked app may read, named after its title. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PdfShareTest {
    /**
     * See ShareWithPhotosTest: each test has its own data folder, which FileProvider must read again; forgotten after
     * too, so a later test that doesn't clear it isn't sent to this one's folder.
     */
    @Before
    @After
    fun forgetFileProviderFolders() {
        FileProvider::class.java.getDeclaredField("sCache").apply {
            isAccessible = true
        }.let { (it.get(null) as MutableMap<*, *>).clear() }
    }

    @Test
    fun aTitleBecomesAFileName() {
        assertEquals("Rainy open ideas", PdfPrint.fileName("Rainy: open ideas"))
        assertEquals("a b c", PdfPrint.fileName(" a/b\\c? "))
        assertEquals("Appkin", PdfPrint.fileName(":?"))
    }

    @Test
    fun aPagelessPageIsA4WideAndAsTallAsItsContentPlusMargins() {
        // 960 CSS pixels are 10 inches (254 mm); with 2 × 16 mm margins and 4 mm of room, 290 mm.
        val media = checkNotNull(PdfPrint.pageless(960f).mediaSize)
        assertEquals(PrintAttributes.MediaSize.ISO_A4.widthMils, media.widthMils)
        assertEquals(11417, media.heightMils)
        assertEquals("a PDF page is at most 200 inches", 200_000, PdfPrint.pageless(1_000_000f).mediaSize?.heightMils)
    }

    @Test
    fun theShareSheetGetsThePdfToRead() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val file = File(activity.cacheDir, "shared/pdf/Rainy open ideas.pdf").apply {
            parentFile?.mkdirs()
            writeText("%PDF")
        }

        val chooser = PdfPrint(activity, "Share").shareIntent(file, "Rainy open ideas")

        val send = checkNotNull(IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent::class.java))
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("application/pdf", send.type)
        val uri = checkNotNull(IntentCompat.getParcelableExtra(send, Intent.EXTRA_STREAM, Uri::class.java))
        assertEquals("content", uri.scheme)
        assertTrue(uri.toString().endsWith("Rainy%20open%20ideas.pdf"))
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals(uri, chooser.clipData?.getItemAt(0)?.uri)
    }
}
