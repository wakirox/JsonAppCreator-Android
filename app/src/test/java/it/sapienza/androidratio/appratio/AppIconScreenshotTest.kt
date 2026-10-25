package it.sapienza.androidratio.appratio

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import it.sapienza.appinterpreter.ui.AppIconView
import it.sapienza.appinterpreter.ui.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

/** Golden images of [AppIconView]: an app's emoji on its soft colour, or its letter tile over cream. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w448dp-h900dp-xxhdpi")
class AppIconScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emojiIcons() = assertScreenshot("app_icon_emoji") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EMOJI_APPS.forEach { (name, icon) -> AppIconView(name, icon) }
        }
    }

    @Test
    fun letterIcons() = assertScreenshot("app_icon_letter") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LETTER_APPS.forEach { (name, icon) -> AppIconView(name, icon) }
        }
    }

    @Test
    fun sizes() = assertScreenshot("app_icon_sizes") { Sizes() }

    @Test
    fun dark() = assertScreenshot("app_icon_dark", darkTheme = true) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppIconView("Top Motorcycles", "🏍️")
            AppIconView("Book club", null)
        }
        Sizes()
    }

    @Composable
    private fun Sizes() {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SIZES.forEach { AppIconView("Recipes", "🍕", it.dp) }
            SIZES.forEach { AppIconView("Recipes", null, it.dp) }
        }
    }

    private fun assertScreenshot(name: String, darkTheme: Boolean = false, content: @Composable () -> Unit) {
        composeRule.setContent {
            AppTheme(darkTheme = darkTheme, dynamicColor = false) {
                Column(
                    Modifier.wrapContentSize(Alignment.TopStart).testTag(ROOT_TAG).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) { content() }
            }
        }
        composeRule.waitForIdle()
        val actual = composeRule.onNodeWithTag(ROOT_TAG).captureToImage().asAndroidBitmap()
            .copy(Bitmap.Config.ARGB_8888, false)

        if (System.getProperty(RECORD_ARGUMENT).toBoolean()) {
            writeBitmap(actual, File(File("src/test/resources/goldens").apply { mkdirs() }, "$name.png"))
            return
        }

        val expectedBytes = checkNotNull(javaClass.classLoader?.getResourceAsStream("goldens/$name.png")) {
            "Missing golden: goldens/$name.png (record with -PrecordScreenshots=true)"
        }.use { it.readBytes() }
        val expected = BitmapFactory.decodeByteArray(expectedBytes, 0, expectedBytes.size)
            .copy(Bitmap.Config.ARGB_8888, false)

        assertEquals("Screenshot width changed for $name", expected.width, actual.width)
        assertEquals("Screenshot height changed for $name", expected.height, actual.height)

        val expectedPixels = IntArray(expected.width * expected.height)
        val actualPixels = IntArray(actual.width * actual.height)
        expected.getPixels(expectedPixels, 0, expected.width, 0, 0, expected.width, expected.height)
        actual.getPixels(actualPixels, 0, actual.width, 0, 0, actual.width, actual.height)

        val diffPixels = IntArray(actualPixels.size)
        var changedPixels = 0
        for (index in expectedPixels.indices) {
            val changed = channelDelta(expectedPixels[index], actualPixels[index]) > CHANNEL_TOLERANCE
            if (changed) changedPixels++
            diffPixels[index] = if (changed) DIFF_COLOR else actualPixels[index]
        }

        val changedFraction = changedPixels.toDouble() / expectedPixels.size
        val outputDirectory = File("build/outputs/app-icon-screenshots").apply { mkdirs() }
        if (changedFraction > MAX_CHANGED_FRACTION) {
            writeBitmap(actual, File(outputDirectory, "$name.actual.png"))
            val diff = Bitmap.createBitmap(actual.width, actual.height, Bitmap.Config.ARGB_8888)
            diff.setPixels(diffPixels, 0, diff.width, 0, 0, diff.width, diff.height)
            writeBitmap(diff, File(outputDirectory, "$name.diff.png"))
        }
        assertTrue(
            "$name changed by ${"%.3f".format(changedFraction * 100)}%. " +
                "Actual and diff images: ${outputDirectory.absolutePath}",
            changedFraction <= MAX_CHANGED_FRACTION
        )
    }

    private fun channelDelta(expected: Int, actual: Int): Int = maxOf(
        abs(android.graphics.Color.alpha(expected) - android.graphics.Color.alpha(actual)),
        abs(android.graphics.Color.red(expected) - android.graphics.Color.red(actual)),
        abs(android.graphics.Color.green(expected) - android.graphics.Color.green(actual)),
        abs(android.graphics.Color.blue(expected) - android.graphics.Color.blue(actual))
    )

    private fun writeBitmap(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }

    companion object {
        private const val ROOT_TAG = "app-icons"
        private const val RECORD_ARGUMENT = "recordScreenshots"
        private const val CHANNEL_TOLERANCE = 12
        private const val MAX_CHANGED_FRACTION = 0.005
        private const val DIFF_COLOR = 0xFFFF00FF.toInt()

        /** Different names, so different soft colours; the motorcycle carries a variation selector. */
        private val EMOJI_APPS = listOf(
            "Top Motorcycles" to "🏍️",
            "Book club" to "📚",
            "Recipes" to "🍕",
            "Travel" to "✈️",
            "Playlist" to "🎵",
            "Padded" to "  🌱  "
        )

        /** No emoji (none, or blank): the first letter or digit, uppercased, or "?" when there is none. */
        private val LETTER_APPS = listOf(
            "Book club" to null,
            "  recipes" to null,
            "Travel" to "   ",
            "42 Club" to null,
            "Zoo" to null,
            "!!!" to null
        )

        private val SIZES = listOf(24, 40, 64)
    }
}
