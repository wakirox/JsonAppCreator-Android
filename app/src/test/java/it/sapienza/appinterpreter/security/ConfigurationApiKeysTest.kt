package it.sapienza.appinterpreter.security

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The placeholder logic itself is tested in :shared (ApiKeyPlaceholdersTest); this is the Android key store side. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ConfigurationApiKeysTest {
    @Test
    fun `decodes build values scoped by bundled configuration`() {
        val json = """{"v3_the_movie_db":{"TMDB_API_KEY":"test-only-value"},""" +
            """"other":{"SERVICE_TOKEN":"second-test-value"}}"""
        val encoded = Base64.getEncoder().encodeToString(json.toByteArray())

        val values = ConfigurationApiKeys.decodeBundledValues(encoded)

        assertEquals("test-only-value", values["v3_the_movie_db"]?.get("TMDB_API_KEY"))
        assertEquals("second-test-value", values["other"]?.get("SERVICE_TOKEN"))
    }
}
