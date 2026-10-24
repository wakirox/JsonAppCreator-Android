package it.sapienza.androidratio.appratio

import androidx.test.platform.app.InstrumentationRegistry
import it.sapienza.appinterpreter.utils.PhotoStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Photos kept by JSON apps: references map to files in the app's photos folder, and unused ones are cleaned up. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PhotoStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = PhotoStore(context)
    private val folder = File(context.filesDir, "photos").apply { mkdirs() }

    private fun photo(name: String, modified: Long) = File(folder, name).apply { writeText("jpeg"); setLastModified(modified) }

    @Test
    fun aReferenceIsAFileInThePhotosFolder() {
        assertEquals(File(folder, "a.jpg"), store.file("appkin-photo:a.jpg"))
        assertNull(store.file("https://example.com/a.jpg"))
        assertNull(store.file("appkin-photo:../shared_prefs/AppData.xml"))
    }

    @Test
    fun onlyOldPhotosNothingRefersToAreDeleted() {
        val day = 24L * 60 * 60 * 1000
        // Dated ahead of the real clock, so the app's own clean-up at start (on another thread) leaves them alone.
        val now = System.currentTimeMillis() + 100 * day
        val used = photo("used.jpg", now - 10 * day)
        val unused = photo("unused.jpg", now - 10 * day)
        val fresh = photo("fresh.jpg", now - day / 2)

        assertEquals(1, store.deleteUnused(setOf("appkin-photo:used.jpg"), now))

        assertTrue(used.exists())
        assertFalse(unused.exists())
        assertTrue("a photo picked in a form not saved yet stays for a day", fresh.exists())
    }
}
