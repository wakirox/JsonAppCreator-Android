package it.sapienza.androidratio.appratio

import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.imageDecoderEnabled
import coil3.map.Mapper
import it.sapienza.appinterpreter.utils.PhotoStore
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import android.graphics.Shader
import android.graphics.Paint
import android.graphics.LinearGradient
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import it.sapienza.appinterpreter.activity.MainActivity
import it.sapienza.appinterpreter.activity.PhotoActivity
import it.sapienza.appinterpreter.activity.createIntentPhotoActivity
import it.sapienza.appinterpreter.storage.AppData
import it.sapienza.appinterpreter.utils.AppLibrary
import it.sapienza.appinterpreter.utils.SharedPreferencesStore
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
import java.nio.file.Files
import java.nio.file.Path

/**
 * Golden images of the App ideas example (res/raw/v3_app_ideas.json) with saved apps and ideas: the apps, and one app's ideas with one ticked,
 * in light and dark. Compared exactly: any changed pixel fails.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w448dp-h900dp-mdpi")
class AppIdeasScreenshotTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private lateinit var scenario: ActivityScenario<out Activity>

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Before
    fun setUp() {
        // Robolectric has no ImageDecoder: decode with BitmapFactory, and read the app's photos as the app does.
        val photos = PhotoStore(context)
        SingletonImageLoader.setUnsafe(
            ImageLoader.Builder(context).imageDecoderEnabled(false)
                .components { add(Mapper<String, File> { data, _ -> photos.file(data) }) }
                .build()
        )
        writePhoto("sky.jpg", 0xFF3D7BD9.toInt(), 0xFFB8D8F5.toInt())
        writePhoto("rain.jpg", 0xFF2E3A59.toInt(), 0xFF7FA6C9.toInt())
        preferences.edit().putString(CONFIGURATION_KEY, Files.readString(Path.of("src", "main", "res", "raw", "v3_app_ideas.json"))).commit()
        val ids = listOf("app-rainy", "app-tasse", "idea-1", "idea-2", "idea-3", "idea-4").iterator()
        val data = AppData(SharedPreferencesStore(context, DATA_PREFERENCES), now = { 1_761_300_000_000 }, newId = { ids.next() })
        val app = AppLibrary.idOf("App ideas")
        data.add(app, "apps", item("name" to "Rainy", "description" to "Rain alerts and forecasts, on the phone and the watch"))
        data.add(
            app, "apps",
            item(
                "name" to "Tasse Forfettario",
                "description" to "Taxes for Italy's flat-rate scheme, worked out from a year's invoices\nAndroid and iPhone, Duo included"
            )
        )
        data.add(app, "ideas", item("title" to "Dark mode", "priority" to "Medium", "done" to true, "appId" to "app-rainy"))
        data.add(
            app, "ideas",
            // Edge cases: a title long enough to wrap, a description over several lines.
            item(
                "title" to "Widgets for the home screen, the lock screen and the watch face, in every size",
                "description" to "Today's rain at a glance\nTomorrow's forecast underneath\nA tap opens the app ☔",
                "priority" to "Low", "done" to false, "appId" to "app-rainy",
                "tags" to JsonArray(listOf(JsonPrimitive("widgets"), JsonPrimitive("ui"))),
                // A photo whose file is gone (lost, say): it shows as unavailable, not as a gap.
                "photos" to JsonArray(listOf(JsonPrimitive("appkin-photo:missing.jpg")))
            )
        )
        data.add(
            app, "ideas",
            item(
                "title" to "Rain alerts on the watch", "description" to "A tap on the wrist before it rains\nOnly when I'm outside", "priority" to "High",
                "appId" to "app-rainy", "done" to false, "link" to "https://wakirox.app",
                "tags" to JsonArray(listOf(JsonPrimitive("watch"), JsonPrimitive("alerts"))),
                // Changed a day after it was added: a fixed date, as both are more than a week ago.
                "updatedAt" to JsonPrimitive(1_761_386_400_000),
                "photos" to JsonArray(listOf(JsonPrimitive("appkin-photo:sky.jpg"), JsonPrimitive("appkin-photo:rain.jpg")))
            )
        )
        data.add(app, "ideas", item("title" to "Export to PDF", "priority" to "High", "done" to false, "appId" to "app-tasse"))
        scenario = ActivityScenario.launch(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    @After
    fun tearDown() {
        scenario.close()
        preferences.edit().remove(CONFIGURATION_KEY).commit()
        context.getSharedPreferences(DATA_PREFERENCES, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun apps() {
        awaitText("Tasse Forfettario")
        assertScreenshot("app_ideas_apps")
    }

    @Test
    @Config(qualifiers = DARK_QUALIFIERS)
    fun appsDark() {
        awaitText("Tasse Forfettario")
        assertScreenshot("app_ideas_apps_dark")
    }

    @Test
    fun ideas() {
        openRainy()
        assertScreenshot("app_ideas_ideas")
    }

    @Test
    @Config(qualifiers = DARK_QUALIFIERS)
    fun ideasDark() {
        openRainy()
        assertScreenshot("app_ideas_ideas_dark")
    }

    @Test
    fun swipingAnIdeaAwayAsksBeforeDeletingIt() {
        openRainy()
        composeRule.onNodeWithTag("list-item-idea-3").performTouchInput { swipeLeft() }
        awaitText("Delete Rain alerts on the watch?")
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Rain alerts on the watch").assertExists()

        composeRule.onNodeWithTag("list-item-idea-3").performTouchInput { swipeLeft() }
        awaitText("Delete Rain alerts on the watch?")
        composeRule.onNodeWithText("OK").performClick()
        composeRule.waitUntil(WAIT_TIMEOUT_MS) {
            composeRule.onAllNodesWithText("Rain alerts on the watch", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun swipingAnIdeaTowardsTheEndMarksItDone() {
        openRainy()
        composeRule.onNodeWithTag("list-item-idea-3").performTouchInput { swipeRight() }
        composeRule.waitUntil(WAIT_TIMEOUT_MS) {
            AppData(SharedPreferencesStore(context, DATA_PREFERENCES)).items(AppLibrary.idOf("App ideas"), "ideas")
                .first { it["id"].toString() == "\"idea-3\"" }["done"].toString() == "true"
        }
    }

    @Test
    fun anIdeasPhotosCanBeRemovedOnItsEditScreen() {
        openRainy()
        openTapped("list-item-idea-3")
        awaitText("Edit idea")
        awaitTag("image-appkin-photo:sky.jpg")
        assertScreenshot("app_ideas_edit")
        assertEquals(2, composeRule.onAllNodesWithContentDescription("Remove photo").fetchSemanticsNodes().size)

        composeRule.onAllNodesWithContentDescription("Remove photo")[0].performClick()
        composeRule.onNodeWithText("Save changes").performScrollTo().performClick()

        composeRule.waitUntil(WAIT_TIMEOUT_MS) {
            AppData(SharedPreferencesStore(context, DATA_PREFERENCES)).items(AppLibrary.idOf("App ideas"), "ideas")
                .first { it["id"].toString() == "\"idea-3\"" }["photos"].toString() == "[\"appkin-photo:rain.jpg\"]"
        }
    }

    @Test
    fun aNewAppIsWrittenOnItsOwnScreen() {
        awaitText("Tasse Forfettario")
        composeRule.onNodeWithText("New app").performClick()
        openNext()
        awaitText("Description")
        assertScreenshot("app_ideas_new_app")
    }

    @Test
    fun aNewIdeaIsWrittenOnItsOwnScreen() {
        openRainy()
        composeRule.onNodeWithText("New idea").performClick()
        openNext()
        awaitText("New idea for Rainy")
        assertScreenshot("app_ideas_new_idea")
    }

    @Test
    fun aSwipedCardThatStaysInPlaceSlidesBack() {
        openRainy()
        // Dark mode is done already and last: swiping it done again changes nothing about its place in the list.
        val card = composeRule.onNodeWithTag("list-item-idea-1")
        val left = card.fetchSemanticsNode().boundsInRoot.left
        card.performTouchInput { swipeRight() }
        val saved = { AppData(SharedPreferencesStore(context, DATA_PREFERENCES)).items(AppLibrary.idOf("App ideas"), "ideas") }
        composeRule.waitUntil(WAIT_TIMEOUT_MS) { saved().first { it["id"].toString() == "\"idea-1\"" }["updatedAt"] != null }
        val firstUpdate = saved().first { it["id"].toString() == "\"idea-1\"" }["updatedAt"].toString()
        // A swipe runs its action once: the slide back must not settle the card swiped again (which did, endlessly).
        composeRule.mainClock.advanceTimeBy(1_500)
        assertEquals("one swipe, one update", firstUpdate, saved().first { it["id"].toString() == "\"idea-1\"" }["updatedAt"].toString())
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("list-item-idea-1").fetchSemanticsNodes().singleOrNull()?.boundsInRoot?.left?.let { kotlin.math.abs(it - left) < 1f } == true
        }
    }

    @Test
    fun aPhotoOpensFullScreenUnderASeeThroughBar() {
        scenario.close()
        val sky = checkNotNull(PhotoStore(context).file("appkin-photo:sky.jpg"))
        scenario = ActivityScenario.launch<PhotoActivity>(createIntentPhotoActivity(context, Uri.fromFile(sky).toString()))
        awaitTag("photo-loaded")
        assertScreenshot("app_ideas_photo")
    }

    /** Taps [tag] and moves to the screen it opens (Robolectric doesn't start activities on its own). */
    private fun openTapped(tag: String) {
        composeRule.onNodeWithTag(tag).performClick()
        openNext()
    }

    private fun openNext() {
        var next: Intent? = null
        composeRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) {
            instrumentation.runOnMainSync { next = shadowOf(resumed()).nextStartedActivity }
            next != null
        }
        scenario.close()
        scenario = ActivityScenario.launch(checkNotNull(next))
    }

    private fun openRainy() {
        awaitText("Rainy")
        openTapped("list-item-app-rainy")
        awaitText("Widgets for the home screen, the lock screen and the watch face, in every size")
        awaitTag("image-appkin-photo:sky.jpg")
        awaitTag("image-appkin-photo:rain.jpg")
        composeRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) {
            composeRule.onAllNodesWithContentDescription("Image unavailable").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
    }

    private fun awaitTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
    }

    private fun item(vararg values: Pair<String, Any>) = JsonObject(
        values.associate { (key, value) ->
            key to when (value) {
                is JsonElement -> value
                is Boolean -> JsonPrimitive(value)
                else -> JsonPrimitive(value.toString())
            }
        }
    )

    /** A small photo in the app's photos folder: a diagonal blend of [from] and [to]. */
    private fun writePhoto(name: String, from: Int, to: Int) {
        val bitmap = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, 160f, 160f, from, to, Shader.TileMode.CLAMP) })
        val file = checkNotNull(PhotoStore(context).file("appkin-photo:$name"))
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    }

    private fun resumed(): Activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).last()

    private fun awaitText(text: String) {
        composeRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) {
            composeRule.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
    }

    private fun assertScreenshot(name: String) {
        composeRule.waitForIdle()
        var captured: Bitmap? = null
        instrumentation.runOnMainSync {
            val view = resumed().window.decorView
            captured = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
        }
        val actual = checkNotNull(captured)

        if (System.getProperty(RECORD_ARGUMENT).toBoolean()) {
            writeBitmap(actual, File(File("src/test/resources/goldens").apply { mkdirs() }, "$name.png"))
            return
        }

        val expectedBytes = checkNotNull(javaClass.classLoader?.getResourceAsStream("goldens/$name.png")) {
            "Missing golden: goldens/$name.png (record with -PrecordScreenshots=true)"
        }.use { it.readBytes() }
        val expected = BitmapFactory.decodeByteArray(expectedBytes, 0, expectedBytes.size).copy(Bitmap.Config.ARGB_8888, false)

        assertEquals("Screenshot width changed for $name", expected.width, actual.width)
        assertEquals("Screenshot height changed for $name", expected.height, actual.height)

        val expectedPixels = IntArray(expected.width * expected.height)
        val actualPixels = IntArray(actual.width * actual.height)
        expected.getPixels(expectedPixels, 0, expected.width, 0, 0, expected.width, expected.height)
        actual.getPixels(actualPixels, 0, actual.width, 0, 0, actual.width, actual.height)
        val changedPixels = expectedPixels.indices.count { expectedPixels[it] != actualPixels[it] }

        val outputDirectory = File("build/outputs/app-ideas-screenshots").apply { mkdirs() }
        if (changedPixels > 0) {
            writeBitmap(actual, File(outputDirectory, "$name.actual.png"))
            val diffPixels = IntArray(actualPixels.size) { if (expectedPixels[it] != actualPixels[it]) DIFF_COLOR else actualPixels[it] }
            val diff = Bitmap.createBitmap(actual.width, actual.height, Bitmap.Config.ARGB_8888)
            diff.setPixels(diffPixels, 0, diff.width, 0, 0, diff.width, diff.height)
            writeBitmap(diff, File(outputDirectory, "$name.diff.png"))
        }
        assertTrue("$name changed in $changedPixels pixels. Actual and diff images: ${outputDirectory.absolutePath}", changedPixels == 0)
    }

    private fun writeBitmap(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }

    companion object {
        private const val PREFERENCES_NAME = "DomainController"
        private const val CONFIGURATION_KEY = "main_file_sav"
        private const val DATA_PREFERENCES = "AppData"
        private const val RECORD_ARGUMENT = "recordScreenshots"
        private const val DARK_QUALIFIERS = "w448dp-h900dp-night-mdpi"
        private const val WAIT_TIMEOUT_MS = 30_000L
        private const val DIFF_COLOR = 0xFFFF00FF.toInt()
    }
}
