package it.sapienza.androidratio.appratio

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import it.sapienza.appinterpreter.activity.MainActivity
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Collections

/** A grid list from a request, which loads its next page when its end comes into view; an empty list's text. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w448dp-h900dp-mdpi")
class ListPagesAndGridTest {
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferences = targetContext.getSharedPreferences("DomainController", Context.MODE_PRIVATE)
    private var original: String? = null
    private val server = MockWebServer()
    private val paths: MutableList<String> = Collections.synchronizedList(mutableListOf())
    private var scenario: ActivityScenario<MainActivity>? = null

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Before
    fun setUp() {
        original = preferences.getString(KEY, null)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                paths += request.path.orEmpty()
                val url = request.requestUrl!!
                if (url.queryParameter("q") == "nothing") return MockResponse().setBody("""{ "data": [] }""")
                val skip = url.queryParameter("skip")?.toInt() ?: 0
                val items = (skip until minOf(skip + 4, 6)).joinToString { """{ "id": "$it", "title": "Work $it" }""" }
                return MockResponse().setBody("""{ "data": [ $items ] }""")
            }
        }
        server.start()
    }

    @After
    fun tearDown() {
        scenario?.close()
        server.shutdown()
        preferences.edit().apply { if (original == null) remove(KEY) else putString(KEY, original) }.commit()
    }

    private fun open(q: String) {
        preferences.edit().putString(KEY, configuration(q)).commit()
        scenario = ActivityScenario.launch(Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    @Test
    fun aGridLoadsItsNextPageAtItsEnd() {
        open("fuji")
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Work 5").fetchSemanticsNodes().isNotEmpty() }

        assertEquals(listOf("/works?q=fuji&limit=4", "/works?q=fuji&limit=4&skip=4"), paths.toList())
        val first = composeRule.onNodeWithText("Work 0").getUnclippedBoundsInRoot()
        val second = composeRule.onNodeWithText("Work 1").getUnclippedBoundsInRoot()
        val third = composeRule.onNodeWithText("Work 2").getUnclippedBoundsInRoot()
        assertEquals("side by side", first.top, second.top)
        assertNotEquals("the next row", first.top, third.top)
        assertEquals(first.left, third.left)
    }

    @Test
    fun anEmptyListSaysWhatItsTextSays() {
        open("nothing")
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Nothing found for nothing").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("No data available").assertDoesNotExist()
    }

    private fun configuration(q: String) = """
        {
          "schemaVersion": 3, "name": "Pages test", "version": "1.0",
          "mainView": {
            "type": "layout",
            "data": { "q": "$q" },
            "views": [
              { "type": "text", "label": "Results" },
              { "type": "list", "mapping": "data", "orientation": "grid", "columns": 2, "emptyText": "Nothing found for ${'$'}{q}",
                "initData": { "url": "${server.url("/works")}?q=${'$'}{q}&limit=4", "nextPage": { "parameter": "skip", "step": 4 } },
                "itemView": { "type": "text", "mapping": "title" } }
            ]
          }
        }
    """.trimIndent()

    private companion object {
        const val KEY = "main_file_sav"
    }
}
