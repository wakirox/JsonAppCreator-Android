package it.sapienza.appinterpreter.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApiKeyPlaceholdersTest {
    @Test
    fun findsUniqueNamedPlaceholdersWithoutTreatingDataBindingsAsKeys() {
        val json = """{"url":"https://example.test?q=${'$'}{query}&key=__SEARCH_KEY__", """ +
            """"other":"__SEARCH_KEY__", "b":"__A_KEY__"}"""

        assertEquals(listOf("A_KEY", "SEARCH_KEY"), ApiKeyPlaceholders.names(json).toList())
    }

    @Test
    fun replacesOnlyValuesThatWereProvided() {
        val text = "https://example.test?first=__FIRST_KEY__&second=__SECOND_KEY__"

        assertEquals(
            "https://example.test?first=private-value&second=__SECOND_KEY__",
            ApiKeyPlaceholders.replace(text, mapOf("FIRST_KEY" to "private-value"))
        )
    }

    @Test
    fun configurationIdentityIsDeterministicAndSensitiveToUnchangedJson() {
        val first = ApiKeyPlaceholders.configurationId("{\"name\":\"one\"}")

        assertEquals(first, ApiKeyPlaceholders.configurationId("{\"name\":\"one\"}"))
        assertFalse(first == ApiKeyPlaceholders.configurationId("{ \"name\":\"one\"}"))
        assertTrue(first.startsWith("file:"))
    }

    @Test
    fun sha256MatchesTheStandardTestVectors() {
        fun hex(text: String) = Sha256.digest(text.encodeToByteArray()).joinToString("") {
            (it.toInt() and 0xff).toString(16).padStart(2, '0')
        }
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hex(""))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hex("abc"))
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            hex("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq")
        )
        assertEquals("cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0", hex("a".repeat(1_000_000)))
        assertEquals("file:" + hex("{\"é\":1}"), ApiKeyPlaceholders.configurationId("{\"é\":1}"))
    }
}
