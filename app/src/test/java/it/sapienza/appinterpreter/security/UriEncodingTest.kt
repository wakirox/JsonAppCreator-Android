package it.sapienza.appinterpreter.security

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The shared percent-encoding of API keys matches Android's Uri.encode, which the app used before. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UriEncodingTest {
    @Test
    fun encodesAsUriEncode() {
        listOf("plain-Key_1.0~", "a b&c=d/e?f#g", "ü€😀", "!*'()", "%2F", "+;,:@\$").forEach { text ->
            assertEquals(text, Uri.encode(text), UriEncoding.encode(text))
        }
    }
}
