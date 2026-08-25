package it.sapienza.appinterpreter.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ApiKeyPlaceholdersTest {
    @Test
    fun `finds unique named placeholders without treating data bindings as keys`() {
        val json = """{"url":"https://example.test?q=${'$'}{query}&key=__SEARCH_KEY__", "other":"__SEARCH_KEY__"}"""

        assertEquals(setOf("SEARCH_KEY"), ApiKeyPlaceholders.names(json))
    }

    @Test
    fun `replaces only values that were provided`() {
        val text = "https://example.test?first=__FIRST_KEY__&second=__SECOND_KEY__"

        val resolved = ApiKeyPlaceholders.replace(text, mapOf("FIRST_KEY" to "private-value"))

        assertEquals(
            "https://example.test?first=private-value&second=__SECOND_KEY__",
            resolved
        )
    }

    @Test
    fun `configuration identity is deterministic and sensitive to unchanged JSON`() {
        val first = ApiKeyPlaceholders.configurationId("{\"name\":\"one\"}")

        assertEquals(first, ApiKeyPlaceholders.configurationId("{\"name\":\"one\"}"))
        assertFalse(first == ApiKeyPlaceholders.configurationId("{ \"name\":\"one\"}"))
        assertTrue(first.startsWith("file:"))
    }

    @Test
    fun `decodes build values scoped by bundled configuration`() {
        val json = """{"v3_the_movie_db":{"TMDB_API_KEY":"test-only-value"},"other":{"SERVICE_TOKEN":"second-test-value"}}"""
        val encoded = Base64.getEncoder().encodeToString(json.toByteArray())

        val values = ConfigurationApiKeys.decodeBundledValues(encoded)

        assertEquals("test-only-value", values["v3_the_movie_db"]?.get("TMDB_API_KEY"))
        assertEquals("second-test-value", values["other"]?.get("SERVICE_TOKEN"))
    }
}
