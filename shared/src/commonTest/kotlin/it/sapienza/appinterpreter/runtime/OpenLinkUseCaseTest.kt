package it.sapienza.appinterpreter.runtime

import it.sapienza.appinterpreter.json.JsonText
import it.sapienza.appinterpreter.model.event.OpenLink
import it.sapienza.appinterpreter.runtime.usecase.OpenLinkUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.serialization.json.JsonObject

/** Which addresses an open event hands to the phone. */
class OpenLinkUseCaseTest {
    private val openLink = OpenLinkUseCase()
    private val museum = JsonText.parse(
        """{ "url": "https://clevelandart.org/art/1930.189", "address": "11150 East Blvd, Cleveland", "phone": "+1 216 421 7350",
             "email": "info@clevelandart.org", "bad": "intent://x#Intent;end", "empty": "" }"""
    ) as JsonObject

    private fun open(url: String) = openLink(OpenLink(url), museum)

    private fun ready(url: String): OpenLinkUseCase.Result.Ready = assertIs(open(url))

    @Test
    fun webMapPhoneAndEmailAddressesOpen() {
        assertEquals("https://clevelandart.org/art/1930.189", ready("${'$'}{url}").url)
        assertEquals("http://example.com", ready("http://example.com").url)
        assertEquals("geo:0,0?q=11150%20East%20Blvd%2C%20Cleveland", ready("geo:0,0?q=${'$'}{address}").url)
        assertEquals("tel", ready("tel:${'$'}{phone}").scheme)
        assertEquals("tel:%2B1%20216%20421%207350", ready("tel:${'$'}{phone}").url)
        assertEquals("mailto:info%40clevelandart.org", ready("mailto:${'$'}{email}").url)
        assertEquals("https", ready("HTTPS://example.com").scheme)
    }

    @Test
    fun otherAddressesDoNot() {
        listOf(
            "javascript:alert(1)",
            "intent://x#Intent;end",
            "file:///sdcard/a",
            "content://x/y",
            "https:",
            "localhost",
            "${'$'}{bad}",
            "${'$'}{empty}"
        )
            .forEach { assertIs<OpenLinkUseCase.Result.NotAllowed>(open(it), it) }
    }

    @Test
    fun aWebAddressTypedWithoutItsSchemeIsAWebPage() {
        listOf("example.com", "www.example.com/a?b=1", "play.google.com/store/apps/details?id=x").forEach {
            val ready = assertIs<OpenLinkUseCase.Result.Ready>(open(it), it)
            assertEquals("https://$it", ready.url)
            assertEquals("https", ready.scheme)
        }
    }

    @Test
    fun aMissingValueIsNamed() {
        assertEquals("creators.0.url", assertIs<OpenLinkUseCase.Result.Missing>(open("${'$'}{creators.0.url}")).path)
        assertIs<OpenLinkUseCase.Result.Missing>(openLink(OpenLink("tel:${'$'}{phone}"), null))
    }
}
