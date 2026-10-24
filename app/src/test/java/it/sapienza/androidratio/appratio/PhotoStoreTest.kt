package it.sapienza.androidratio.appratio

import android.content.Context
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import it.sapienza.appinterpreter.utils.PhotoStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Kept photos: a thumbnail of their own, a link to the original (or, without one, a copy), the best version for full
 * screen, and clean-up of what no saved data refers to.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PhotoStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = PhotoStore(context)
    private val folder = File(context.filesDir, "photos").apply { mkdirs() }
    private val copies = File(folder, "full").apply { mkdirs() }
    private val links = context.getSharedPreferences(PhotoStore.LINKS, Context.MODE_PRIVATE)

    private fun thumbnail(name: String, modified: Long = System.currentTimeMillis()) =
        File(folder, name).apply { writeText("thumb"); setLastModified(modified) }

    @Test
    fun aReferenceIsItsThumbnail() {
        assertEquals(File(folder, "a.jpg"), store.file("appkin-photo:a.jpg"))
        assertNull(store.file("https://example.com/a.jpg"))
        assertNull(store.file("appkin-photo:../shared_prefs/AppData.xml"))
    }

    @Test
    fun fullScreenFallsBackFromAGoneOriginalToTheCopyThenTheThumbnail() = runTest {
        thumbnail("a.jpg")
        // A link that no longer reads: the photo was deleted from the gallery, or this is a new phone.
        links.edit().putString("a.jpg", "content://media/picker/0/gone").commit()
        assertEquals(Uri.fromFile(File(folder, "a.jpg")).toString(), store.fullSize("appkin-photo:a.jpg"))

        File(copies, "a.jpg").writeText("copy")
        assertEquals(Uri.fromFile(File(copies, "a.jpg")).toString(), store.fullSize("appkin-photo:a.jpg"))
    }

    @Test
    fun aRequestSendsTheBestVersionThereIs() = runTest {
        thumbnail("b.jpg")
        assertEquals("thumb", store.reader.jpeg("appkin-photo:b.jpg")?.decodeToString())
        File(copies, "b.jpg").writeText("copy")
        assertEquals("copy", store.reader.jpeg("appkin-photo:b.jpg")?.decodeToString())
        assertNull(store.reader.jpeg("appkin-photo:nothing.jpg"))
    }

    @Test
    fun onlyOldPhotosNothingRefersToAreForgottenWithTheirCopyAndLink() {
        val day = 24L * 60 * 60 * 1000
        // Dated ahead of the real clock, so the app's own clean-up at start (on another thread) leaves them alone.
        val now = System.currentTimeMillis() + 100 * day
        val used = thumbnail("used.jpg", now - 10 * day)
        val unused = thumbnail("unused.jpg", now - 10 * day)
        val unusedCopy = File(copies, "unused.jpg").apply { writeText("copy") }
        links.edit().putString("unused.jpg", "content://media/picker/0/1").commit()
        val fresh = thumbnail("fresh.jpg", now - day / 2)

        assertEquals(1, store.deleteUnused(setOf("appkin-photo:used.jpg"), now))

        assertTrue(used.exists())
        assertFalse(unused.exists())
        assertFalse(unusedCopy.exists())
        assertNull(links.getString("unused.jpg", null))
        assertTrue("a photo picked in a form not saved yet stays for a day", fresh.exists())
    }
}
