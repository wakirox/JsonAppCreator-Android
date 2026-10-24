package it.sapienza.appinterpreter.runtime

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import it.sapienza.appinterpreter.json.JsonText
import it.sapienza.appinterpreter.json.getValue
import it.sapienza.appinterpreter.model.event.RESTService
import it.sapienza.appinterpreter.runtime.usecase.FetchJsonUseCase
import it.sapienza.appinterpreter.runtime.usecase.MissingValueException
import it.sapienza.appinterpreter.utils.httpclient.JsonHttpClient
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** A request's URL is filled in from the data, and isn't sent at all when the data lacks one of its values. */
class FetchJsonUseCaseTest {
    private val requested = mutableListOf<String>()
    private val sentHeaders = mutableListOf<String>()
    private val fetch = FetchJsonUseCase(
        JsonHttpClient(MockEngine { request ->
            requested += request.url.toString()
            sentHeaders += request.headers["X-Artwork"].orEmpty()
            respond("""{ "ok": "yes" }""")
        }, "Appkin/test")
    )
    private val artwork = JsonText.parse("""{ "creators": [ { "id": 42 } ], "creatorsless": [] }""") as JsonObject

    @Test
    fun aUrlWithEveryValueIsRequested() = runTest {
        val answer = fetch(RESTService("https://example.com/creators/${'$'}{creators.0.id}"), artwork)

        assertEquals("yes", answer.getValue("ok"))
        assertEquals(listOf("https://example.com/creators/42"), requested)
    }

    @Test
    fun valuesAreSentPercentEncodedAndOnlyOnce() = runTest {
        val form = JsonText.parse("""{ "type": "arms & armor", "q": "São #1" }""") as JsonObject

        fetch(RESTService("https://example.com/api/?type=${'$'}{type}&q=${'$'}{q}&has_image=1"), form)

        assertEquals(listOf("https://example.com/api/?type=arms%20%26%20armor&q=S%C3%A3o%20%231&has_image=1"), requested)
    }

    @Test
    fun aUrlWithAMissingValueIsNotRequested() = runTest {
        val error = assertFailsWith<MissingValueException> {
            fetch(RESTService("https://example.com/creators/${'$'}{creatorsless.0.id}?q=${'$'}{q}"), artwork)
        }

        assertEquals("This item has no creatorsless.0.id", error.message)
        assertEquals(emptyList(), requested)
    }

    @Test
    fun withoutDataEveryPlaceholderIsMissing() = runTest {
        assertFailsWith<MissingValueException> { fetch(RESTService("https://example.com/${'$'}{id}"), null) }
        fetch(RESTService("https://example.com/plain"), null)
        assertEquals(listOf("https://example.com/plain"), requested)
    }

    @Test
    fun headersAreFilledInLikeTheUrl() = runTest {
        fetch(RESTService("https://example.com/a", headers = mapOf("X-Artwork" to "creator ${'$'}{creators.0.id}")), artwork)
        assertEquals(listOf("creator 42"), sentHeaders)

        val error = assertFailsWith<MissingValueException> {
            fetch(RESTService("https://example.com/a", headers = mapOf("X-Artwork" to "${'$'}{creatorsless.0.id}")), artwork)
        }
        assertEquals("creatorsless.0.id", error.path)
        assertEquals(1, requested.size)
    }
}
