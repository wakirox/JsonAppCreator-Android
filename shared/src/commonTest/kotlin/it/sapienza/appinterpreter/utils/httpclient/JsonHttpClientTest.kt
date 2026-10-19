package it.sapienza.appinterpreter.utils.httpclient

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JsonHttpClientTest {
    private val requests = mutableListOf<String>()

    private fun client(status: HttpStatusCode = HttpStatusCode.OK, body: String = """{"ok":true}""") = JsonHttpClient(
        MockEngine { request ->
            requests += "${request.method.value} ${request.url} ${request.headers[HttpHeaders.UserAgent]} ${request.body.toByteArray().decodeToString()}"
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        },
        JsonHttpClient.userAgent("1.2", "Test"),
        resolveUrl = { it.replace("__KEY__", "secret") }
    )

    @Test
    fun getsWithTheAppsUserAgentAndKeys() = runTest {
        val answer = client().request("https://example.com/a?k=__KEY__", post = false, body = null)
        assertEquals("true", answer["ok"]?.jsonPrimitive?.content)
        assertEquals(
            "GET https://example.com/a?k=secret Appkin/1.2 (Test; https://github.com/wakirox/JsonAppCreator-Android) ",
            requests.single()
        )
    }

    @Test
    fun postsTheDataAsJson() = runTest {
        client().request("https://example.com/p", post = true, body = JsonObject(mapOf("name" to JsonPrimitive("Ana"))))
        assertEquals("""{"name":"Ana"}""", requests.single().substringAfterLast(' '))
        client().request("https://example.com/p", post = true, body = null)
        assertEquals("null", requests.last().substringAfterLast(' '))
    }

    @Test
    fun failuresSayWhatWentWrong() = runTest {
        assertEquals("Request failed with 503", assertFailsWith<IOException> { client(HttpStatusCode.ServiceUnavailable).request("https://e.com", false, null) }.message)
        assertEquals("The response is not a JSON object", assertFailsWith<IOException> { client(body = "[1]").request("https://e.com", false, null) }.message)
        assertFailsWith<IOException> { client(body = "<html>").request("https://e.com", false, null) }
        assertEquals(
            "Missing configuration API key: OTHER_KEY",
            assertFailsWith<IllegalStateException> { client().request("https://e.com?k=__OTHER_KEY__", false, null) }.message
        )
        assertEquals(3, requests.size, "no request without its key")
    }
}
