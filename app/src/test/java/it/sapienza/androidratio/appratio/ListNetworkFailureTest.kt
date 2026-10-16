package it.sapienza.androidratio.appratio

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import it.sapienza.appinterpreter.activity.MainActivity
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w448dp-h900dp-mdpi")
class ListNetworkFailureTest {

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferences =
        targetContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val server = MockWebServer()
    private val requestCount = AtomicInteger()
    private val releaseFirstResponse = CountDownLatch(1)

    private var hadOriginalConfiguration = false
    private var originalConfiguration: String? = null
    private lateinit var scenario: ActivityScenario<MainActivity>

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Before
    fun setUp() {
        hadOriginalConfiguration = preferences.contains(CONFIGURATION_KEY)
        originalConfiguration = preferences.getString(CONFIGURATION_KEY, null)

        server.start()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return if (requestCount.incrementAndGet() == 1) {
                    check(releaseFirstResponse.await(5, TimeUnit.SECONDS))
                    MockResponse().setResponseCode(503)
                } else {
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("""{"results":[{"id":1,"title":"Recovered item"}]}""")
                }
            }
        }

        val configuration = CONFIGURATION.replace("{{BASE_URL}}", server.url("/").toString())
        preferences.edit().putString(CONFIGURATION_KEY, configuration).commit()
        scenario = ActivityScenario.launch(
            Intent(targetContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    @After
    fun tearDown() {
        releaseFirstResponse.countDown()
        scenario.close()
        if (hadOriginalConfiguration) {
            preferences.edit().putString(CONFIGURATION_KEY, originalConfiguration).commit()
        } else {
            preferences.edit().remove(CONFIGURATION_KEY).commit()
        }
        server.shutdown()
    }

    @Test
    fun activeFailureIsInlineAndRetryRecovers() {
        awaitRequestCount(1)
        releaseFirstResponse.countDown()

        awaitTag("list-load-error")
        assertNull(ShadowAlertDialog.getLatestAlertDialog())

        composeRule.onNodeWithText("Retry").performClick()
        composeRule.waitForIdle()

        awaitRequestCount(2)
        awaitText("Recovered item")
        assertNull(ShadowAlertDialog.getLatestAlertDialog())
    }

    @Test
    fun backgroundFailureRetriesWhenTheAppReturns() {
        awaitRequestCount(1)
        releaseFirstResponse.countDown()
        awaitTag("list-load-error")

        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.moveToState(Lifecycle.State.RESUMED)

        awaitRequestCount(2)
        awaitText("Recovered item")
        assertNull(ShadowAlertDialog.getLatestAlertDialog())
    }

    private fun awaitRequestCount(expected: Int) {
        composeRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) {
            requestCount.get() >= expected
        }
    }

    private fun awaitTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitText(text: String) {
        composeRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) {
            composeRule.onAllNodesWithText(text, substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "DomainController"
        private const val CONFIGURATION_KEY = "main_file_sav"
        private const val WAIT_TIMEOUT_MS = 10_000L
        private val CONFIGURATION = """
            {
              "schemaVersion": 3,
              "name": "Network failure test",
              "author": "Tests",
              "version": "1.0",
              "mainView": {
                "id": "network_failure_home",
                "type": "layout",
                "views": [
                  {
                    "type": "list",
                    "orientation": "horizontal",
                    "initData": {
                      "url": "{{BASE_URL}}items",
                      "method": "get"
                    },
                    "mapping": "results",
                    "itemView": {
                      "type": "text",
                      "mapping": "title"
                    }
                  }
                ]
              }
            }
        """.trimIndent()
    }
}
