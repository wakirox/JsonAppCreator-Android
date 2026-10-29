package it.sapienza.androidratio.appratio

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import androidx.test.platform.app.InstrumentationRegistry
import it.sapienza.appinterpreter.utils.ShareApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Sharing sends the app's JSON, unchanged and under the app's name, as a file the receiving app may read. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ShareAppTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val json = """{ "schemaVersion": 3, "name": "Movies", "version": "1", """ +
        """"mainView": { "type": "text", "text": "__TMDB_API_KEY__" } }"""

    @Test
    fun sendsTheJsonAsANamedFile() {
        val intent = ShareApp.intent(context, "Movies", json)
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("application/json", intent.type)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        val uri = requireNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
        assertEquals("${context.packageName}.files", uri.authority)
        assertEquals("Movies.appkin.json", uri.lastPathSegment)
        // Only the placeholder travels, never a saved key.
        assertEquals(json, context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() })

        // Every share gets its own file. (One test only: FileProvider caches its roots across Robolectric's per-test
        // data folders.)
        assertNotEquals(
            uri,
            IntentCompat.getParcelableExtra(
                ShareApp.intent(context, "Movies", json),
                Intent.EXTRA_STREAM,
                Uri::class.java
            )
        )
    }
}
