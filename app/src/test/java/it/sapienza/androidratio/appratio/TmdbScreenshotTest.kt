package it.sapienza.androidratio.appratio

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.platform.app.InstrumentationRegistry
import it.sapienza.appinterpreter.activity.MainActivity
import okio.Buffer
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w448dp-h900dp-mdpi")
class TmdbScreenshotTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val targetContext = instrumentation.targetContext
    private val server = MockWebServer()
    private val preferences =
        targetContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

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
        server.dispatcher = TmdbDispatcher(
            baseUrl = server.url("/").toString(),
            readAsset = ::readResource
        )

        val fixture = readResource(FIXTURE_ASSET)
            .decodeToString()
            .replace("{{BASE_URL}}", server.url("/").toString())
        preferences.edit().putString(CONFIGURATION_KEY, fixture).commit()

        scenario = ActivityScenario.launch(
            Intent(targetContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    @After
    fun tearDown() {
        scenario.close()
        if (hadOriginalConfiguration) {
            preferences.edit().putString(CONFIGURATION_KEY, originalConfiguration).commit()
        } else {
            preferences.edit().remove(CONFIGURATION_KEY).commit()
        }
        server.shutdown()
    }

    @Test
    fun home() {
        awaitHome()
        assertScreenshot("tmdb_home")
    }

    @Test
    fun searchResults() {
        awaitHome()
        composeRule.onNode(hasSetTextAction()).performTextInput("northstar")
        composeRule.onNodeWithText("Search").performClick()
        launchNextActivity()

        awaitText("Search results")
        awaitTag("image-101")
        awaitTag("image-102")
        assertScreenshot("tmdb_search_results")
    }

    @Test
    fun movieDetails() {
        awaitHome()
        composeRule.onNodeWithTag("list-item-101").performClick()
        launchNextActivity()

        awaitText("A cartographer follows a signal beyond the mapped sky.")
        awaitTag("image-101")
        awaitTag("image-201")
        awaitTag("image-202")
        assertScreenshot("tmdb_movie_details")
    }

    @Test
    fun actorDetails() {
        awaitHome()
        composeRule.onNodeWithTag("list-item-201").performClick()
        launchNextActivity()

        awaitText("Mira Vale builds quiet, character-led science fiction.")
        awaitTag("image-201")
        awaitTag("image-101")
        awaitTag("image-102")
        assertScreenshot("tmdb_actor_details")
    }

    private fun launchNextActivity() {
        var nextIntent: Intent? = null
        composeRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) {
            instrumentation.runOnMainSync {
                val activity = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .filterIsInstance<MainActivity>()
                    .last()
                nextIntent = shadowOf(activity).nextStartedActivity
            }
            nextIntent != null
        }
        scenario.close()
        scenario = ActivityScenario.launch(checkNotNull(nextIntent))
    }

    private fun awaitHome() {
        awaitText("Northstar")
        awaitText("Mira Vale")
        awaitTag("image-101")
        awaitTag("image-102")
        awaitTag("image-201")
        awaitTag("image-202")
    }

    private fun awaitText(text: String) {
        composeRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) {
            composeRule.onAllNodesWithText(text, substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
    }

    private fun assertScreenshot(name: String) {
        composeRule.waitForIdle()
        val actual = captureActivity()

        if (System.getProperty(RECORD_ARGUMENT).toBoolean()) {
            writeBitmap(actual, goldenFile("$name.png"))
            return
        }

        val expectedBytes = readResource("goldens/$name.png")
        val expected = BitmapFactory.decodeByteArray(expectedBytes, 0, expectedBytes.size)
            .copy(Bitmap.Config.ARGB_8888, false)

        assertEquals("Screenshot width changed for $name", expected.width, actual.width)
        assertEquals("Screenshot height changed for $name", expected.height, actual.height)

        val expectedPixels = IntArray(expected.width * expected.height)
        val actualPixels = IntArray(actual.width * actual.height)
        expected.getPixels(expectedPixels, 0, expected.width, 0, 0, expected.width, expected.height)
        actual.getPixels(actualPixels, 0, actual.width, 0, 0, actual.width, actual.height)

        var changedPixels = 0
        val diff = Bitmap.createBitmap(actual.width, actual.height, Bitmap.Config.ARGB_8888)
        val diffPixels = IntArray(actualPixels.size)
        for (index in expectedPixels.indices) {
            val expectedPixel = expectedPixels[index]
            val actualPixel = actualPixels[index]
            val changed = channelDelta(expectedPixel, actualPixel) > CHANNEL_TOLERANCE
            if (changed) changedPixels++
            diffPixels[index] = if (changed) DIFF_COLOR else actualPixel
        }

        val changedFraction = changedPixels.toDouble() / expectedPixels.size
        if (changedFraction > MAX_CHANGED_FRACTION) {
            writeBitmap(actual, outputFile("$name.actual.png"))
            diff.setPixels(diffPixels, 0, diff.width, 0, 0, diff.width, diff.height)
            writeBitmap(diff, outputFile("$name.diff.png"))
        }

        assertTrue(
            "$name changed by ${"%.3f".format(changedFraction * 100)}%. " +
                "Actual and diff images: ${outputDirectory().absolutePath}",
            changedFraction <= MAX_CHANGED_FRACTION
        )
    }

    private fun captureActivity(): Bitmap {
        var captured: Bitmap? = null
        instrumentation.runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<MainActivity>()
                .last()
            val view = activity.window.decorView
            check(view.width > 0 && view.height > 0) { "The activity has not been laid out" }
            captured = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                .also { view.draw(Canvas(it)) }
        }
        return checkNotNull(captured)
    }

    private fun channelDelta(expected: Int, actual: Int): Int = maxOf(
        abs(android.graphics.Color.alpha(expected) - android.graphics.Color.alpha(actual)),
        abs(android.graphics.Color.red(expected) - android.graphics.Color.red(actual)),
        abs(android.graphics.Color.green(expected) - android.graphics.Color.green(actual)),
        abs(android.graphics.Color.blue(expected) - android.graphics.Color.blue(actual))
    )

    private fun outputDirectory(): File =
        File("build/outputs/tmdb-screenshots").apply { mkdirs() }

    private fun outputFile(name: String): File = File(outputDirectory(), name)

    private fun goldenDirectory(): File =
        File("src/test/resources/goldens").apply { mkdirs() }

    private fun goldenFile(name: String): File = File(goldenDirectory(), name)

    private fun readResource(path: String): ByteArray =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(path)) {
            "Missing test resource: $path"
        }.use { it.readBytes() }

    private fun writeBitmap(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }

    private class TmdbDispatcher(
        private val baseUrl: String,
        private val readAsset: (String) -> ByteArray
    ) : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.requestUrl?.encodedPath.orEmpty()
            return when {
                path.startsWith("/images/") -> image(path.removePrefix("/"))
                path == "/popular-movies" -> json(popularMovies())
                path == "/popular-people" -> json(popularPeople())
                path == "/search" -> json(popularMovies())
                path == "/movie/101" -> json(movieDetails())
                path == "/movie/102" -> json(movieDetails())
                path == "/person/201" -> json(actorDetails())
                path == "/person/202" -> json(actorDetails())
                else -> MockResponse().setResponseCode(404)
            }
        }

        private fun json(body: String): MockResponse = MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(body)

        private fun image(assetPath: String): MockResponse = MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "image/png")
            .setBody(Buffer().write(readAsset(assetPath)))

        private fun popularMovies(): String = """
            {
              "results": [
                {
                  "id": 101,
                  "title": "Northstar",
                  "release_date": "2025-11-14",
                  "vote_average": "8.4",
                  "overview": "A cartographer follows a signal beyond the mapped sky.",
                  "poster_path": "${baseUrl}images/poster_northstar.png"
                },
                {
                  "id": 102,
                  "title": "Harbor Lights",
                  "release_date": "2024-06-08",
                  "vote_average": "7.9",
                  "overview": "Two lighthouse keepers decode a message from the tide.",
                  "poster_path": "${baseUrl}images/poster_harbor.png"
                }
              ]
            }
        """.trimIndent()

        private fun popularPeople(): String = """
            {
              "results": [
                {
                  "id": 201,
                  "name": "Mira Vale",
                  "profile_path": "${baseUrl}images/profile_mira.png"
                },
                {
                  "id": 202,
                  "name": "Jonas Reed",
                  "profile_path": "${baseUrl}images/profile_jonas.png"
                }
              ]
            }
        """.trimIndent()

        private fun movieDetails(): String = """
            {
              "id": 101,
              "title": "Northstar",
              "release_date": "2025-11-14",
              "overview": "A cartographer follows a signal beyond the mapped sky.",
              "backdrop_path": "${baseUrl}images/backdrop_northstar.png",
              "credits": {
                "cast": [
                  {
                    "id": 201,
                    "name": "Mira Vale",
                    "profile_path": "${baseUrl}images/profile_mira.png"
                  },
                  {
                    "id": 202,
                    "name": "Jonas Reed",
                    "profile_path": "${baseUrl}images/profile_jonas.png"
                  }
                ]
              }
            }
        """.trimIndent()

        private fun actorDetails(): String = """
            {
              "id": 201,
              "name": "Mira Vale",
              "biography": "Mira Vale builds quiet, character-led science fiction.",
              "profile_path": "${baseUrl}images/profile_mira.png",
              "movie_credits": {
                "cast": [
                  {
                    "id": 101,
                    "title": "Northstar",
                    "poster_path": "${baseUrl}images/poster_northstar.png"
                  },
                  {
                    "id": 102,
                    "title": "Harbor Lights",
                    "poster_path": "${baseUrl}images/poster_harbor.png"
                  }
                ]
              }
            }
        """.trimIndent()
    }

    companion object {
        private const val FIXTURE_ASSET = "tmdb_screenshot_fixture.json"
        private const val PREFERENCES_NAME = "DomainController"
        private const val CONFIGURATION_KEY = "main_file_sav"
        private const val RECORD_ARGUMENT = "recordScreenshots"
        private const val WAIT_TIMEOUT_MS = 30_000L
        private const val CHANNEL_TOLERANCE = 12
        private const val MAX_CHANGED_FRACTION = 0.005
        private const val DIFF_COLOR = 0xFFFF00FF.toInt()
    }
}
