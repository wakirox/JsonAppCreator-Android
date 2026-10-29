package it.sapienza.androidratio.appratio

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.imageDecoderEnabled
import it.sapienza.appinterpreter.activity.MainActivity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
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

/**
 * The Masterpieces example (`res/raw/v3_masterpieces.json`) with its Wikimedia Commons images served by a local server,
 * in light and dark. Compared exactly: any changed pixel fails.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w448dp-h900dp-mdpi")
class MasterpiecesScreenshotTest {

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
    @OptIn(DelicateCoilApi::class)
    fun setUp() {
        // Robolectric has no ImageDecoder, which Coil uses on Android 10 and up: decode with BitmapFactory here.
        SingletonImageLoader.setUnsafe(ImageLoader.Builder(targetContext).imageDecoderEnabled(false).build())
        hadOriginalConfiguration = preferences.contains(CONFIGURATION_KEY)
        originalConfiguration = preferences.getString(CONFIGURATION_KEY, null)

        server.dispatcher = ImageDispatcher()
        server.start()

        val configuration = targetContext.resources.openRawResource(R.raw.v3_masterpieces)
            .use { it.readBytes().decodeToString() }
            .replace(COMMONS_FILES, server.url("/images/").toString())
        preferences.edit().putString(CONFIGURATION_KEY, configuration).commit()

        launchHome()
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
        assertScreenshot("masterpieces_home")
    }

    @Test
    @Config(qualifiers = DARK_QUALIFIERS)
    fun homeDark() {
        awaitHome()
        assertScreenshot("masterpieces_home_dark")
    }

    @Test
    fun artwork() {
        openMonaLisa()
        assertScreenshot("masterpieces_artwork")
    }

    @Test
    @Config(qualifiers = DARK_QUALIFIERS)
    fun artworkDark() {
        openMonaLisa()
        assertScreenshot("masterpieces_artwork_dark")
    }

    @Test
    fun favourites() {
        openFavourites()
        assertScreenshot("masterpieces_favourites")
    }

    @Test
    @Config(qualifiers = DARK_QUALIFIERS)
    fun favouritesDark() {
        openFavourites()
        assertScreenshot("masterpieces_favourites_dark")
    }

    @Test
    fun credits() {
        openCredits()
        assertScreenshot("masterpieces_credits")
    }

    @Test
    @Config(qualifiers = DARK_QUALIFIERS)
    fun creditsDark() {
        openCredits()
        assertScreenshot("masterpieces_credits_dark")
    }

    private fun launchHome() {
        scenario = ActivityScenario.launch(
            Intent(targetContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun openMonaLisa() {
        awaitHome()
        composeRule.onNodeWithText("Mona Lisa").performClick()
        launchNextActivity()
        awaitText("A calm half-smile")
        awaitImages(1)
    }

    private fun openFavourites() {
        openMonaLisa()
        composeRule.onNodeWithText("Favourite").performScrollTo().performClick()
        awaitText("Mona Lisa is in your favourites.")
        scenario.close()
        launchHome()
        awaitHome()
        composeRule.onNodeWithText("Favourites").performClick()
        launchNextActivity()
        awaitText("Leonardo da Vinci")
        awaitImages(1)
    }

    private fun openCredits() {
        awaitHome()
        composeRule.onNodeWithText("Credits").performClick()
        launchNextActivity()
        awaitText("Every image is a public-domain work")
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
        awaitText("Renaissance")
        awaitText("Mona Lisa")
        awaitImages(HOME_IMAGES)
    }

    private fun awaitText(text: String) {
        composeRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) {
            composeRule.onAllNodesWithText(text, substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitImages(count: Int) {
        val loadedImage = SemanticsMatcher("is a loaded image") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("image-") == true
        }
        val loading = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)
        composeRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) {
            composeRule.onAllNodes(loadedImage, useUnmergedTree = true).fetchSemanticsNodes().size >= count &&
                composeRule.onAllNodes(loading, useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
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

        val changedPixels = expectedPixels.indices.count { expectedPixels[it] != actualPixels[it] }
        if (changedPixels > 0) {
            writeBitmap(actual, outputFile("$name.actual.png"))
            val diffPixels = IntArray(actualPixels.size) {
                if (expectedPixels[it] != actualPixels[it]) DIFF_COLOR else actualPixels[it]
            }
            val diff = Bitmap.createBitmap(actual.width, actual.height, Bitmap.Config.ARGB_8888)
            diff.setPixels(diffPixels, 0, diff.width, 0, 0, diff.width, diff.height)
            writeBitmap(diff, outputFile("$name.diff.png"))
        }

        assertTrue(
            "$name changed in $changedPixels pixels. Actual and diff images: ${outputDirectory().absolutePath}",
            changedPixels == 0
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

    private fun outputDirectory(): File = File("build/outputs/masterpieces-screenshots").apply { mkdirs() }

    private fun outputFile(name: String): File = File(outputDirectory(), name)

    private fun goldenFile(name: String): File = File(File("src/test/resources/goldens").apply { mkdirs() }, name)

    private fun readResource(path: String): ByteArray = checkNotNull(javaClass.classLoader?.getResourceAsStream(path)) {
        "Missing test resource: $path"
    }.use { it.readBytes() }

    private fun writeBitmap(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }

    /** Answers every image request with a small picture whose colours come from the requested file name. */
    private class ImageDispatcher : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.requestUrl?.encodedPath.orEmpty()
            if (!path.startsWith("/images/")) return MockResponse().setResponseCode(404)
            return MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "image/png")
                .setBody(Buffer().write(picture(path.removePrefix("/images/"))))
        }

        private fun picture(file: String): ByteArray {
            val hue = abs(file.fold(0) { hash, char -> hash * 31 + char.code } % 360).toFloat()
            val bitmap = Bitmap.createBitmap(PICTURE_WIDTH, PICTURE_HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint()
            canvas.drawColor(Color.HSVToColor(floatArrayOf(hue, 0.35f, 0.9f)))
            paint.color = Color.HSVToColor(floatArrayOf((hue + 40) % 360, 0.6f, 0.55f))
            canvas.drawCircle(PICTURE_WIDTH * 0.65f, PICTURE_HEIGHT * 0.35f, PICTURE_HEIGHT * 0.22f, paint)
            paint.color = Color.HSVToColor(floatArrayOf((hue + 200) % 360, 0.5f, 0.4f))
            canvas.drawRect(0f, PICTURE_HEIGHT * 0.7f, PICTURE_WIDTH.toFloat(), PICTURE_HEIGHT.toFloat(), paint)
            return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        }
    }

    companion object {
        private const val COMMONS_FILES = "https://commons.wikimedia.org/wiki/Special:FilePath/"
        private const val PREFERENCES_NAME = "DomainController"
        private const val CONFIGURATION_KEY = "main_file_sav"
        private const val RECORD_ARGUMENT = "recordScreenshots"
        private const val DARK_QUALIFIERS = "w448dp-h900dp-night-mdpi"
        private const val WAIT_TIMEOUT_MS = 30_000L
        private const val HOME_IMAGES = 3
        private const val PICTURE_WIDTH = 160
        private const val PICTURE_HEIGHT = 120
        private const val DIFF_COLOR = 0xFFFF00FF.toInt()
    }
}
