package it.sapienza.androidratio.appratio

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import androidx.test.platform.app.InstrumentationRegistry
import it.sapienza.appinterpreter.utils.ExternalApps
import it.sapienza.appinterpreter.utils.PhotoStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Sharing an item with its photos: text as the caption, kept photos as readable attachments. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ShareWithPhotosTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = PhotoStore(context)

    /**
     * FileProvider keeps each authority's folders for the whole process, and Robolectric gives every test its own data
     * folder: forget what an earlier test (of any class) left, so this one's folders are read.
     */
    @Before
    fun forgetFileProviderFolders() {
        FileProvider::class.java.getDeclaredField("sCache").apply { isAccessible = true }.let { (it.get(null) as MutableMap<*, *>).clear() }
    }

    private suspend fun photo(name: String): Uri {
        checkNotNull(store.file("appkin-photo:$name")).apply { parentFile?.mkdirs(); writeText("jpeg") }
        return checkNotNull(store.shareable("appkin-photo:$name"))
    }

    private fun Intent.target(): Intent = checkNotNull(IntentCompat.getParcelableExtra(this, Intent.EXTRA_INTENT, Intent::class.java))

    @Test
    fun keptPhotosAreSharedAsACopyThroughTheAppsFileProvider() = runTest {
        val uri = photo("a.jpg")
        assertEquals("content", uri.scheme)
        assertEquals("${context.packageName}.files", uri.authority)
        assertEquals("jpeg", context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() })
        assertNull("a photo that's gone isn't offered", store.shareable("appkin-photo:gone.jpg"))
    }

    @Test
    fun severalPhotosGoTogetherWithTheTextAsCaption() {
        // FileProvider keeps the first test's folders for the whole run, so these tests use plain content addresses.
        val photos = listOf(Uri.parse("content://photos/a.jpg"), Uri.parse("content://photos/b.jpg"))
        val chooser = ExternalApps.share(context, "Tile", "Share", photos)
        val send = chooser.target()

        assertEquals(Intent.ACTION_SEND_MULTIPLE, send.action)
        assertEquals("image/jpeg", send.type)
        assertEquals("Tile", send.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals(photos, IntentCompat.getParcelableArrayListExtra(send, Intent.EXTRA_STREAM, Uri::class.java))
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals(2, checkNotNull(chooser.clipData).itemCount)
    }

    @Test
    fun onePhotoIsASingleAttachmentAndNoneIsPlainText() {
        val one = ExternalApps.share(context, "Tile", "Share", listOf(Uri.parse("content://photos/a.jpg"))).target()
        assertEquals(Intent.ACTION_SEND, one.action)
        assertEquals("image/jpeg", one.type)

        val none = ExternalApps.share(context, "Tile", "Share").target()
        assertEquals("text/plain", none.type)
        assertNull(none.clipData)
    }
}
