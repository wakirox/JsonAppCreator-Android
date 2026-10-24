package it.sapienza.androidratio.appratio

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import it.sapienza.appinterpreter.ai.AskAiScreen
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

/**
 * Golden images of the "Ask your AI app" screen: a new app (empty, and with a request), changing the current app, and
 * the dark theme. Compared exactly: any changed pixel fails.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w448dp-h900dp-xxhdpi")
class AskAiScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun newAppEmpty() = assertScreenshot("ask_ai_new_app", request = "")

    @Test
    fun newAppWithARequest() = assertScreenshot("ask_ai_new_app_request", request = "A list of my favourite books, with a screen for each")

    @Test
    fun changeTheCurrentApp() = assertScreenshot("ask_ai_change_current", request = "Add a screen with the opening hours", changeCurrent = true)

    @Test
    fun withoutACurrentApp() = assertScreenshot("ask_ai_no_current_app", request = "", canChangeCurrent = false)

    @Test
    fun dark() = assertScreenshot("ask_ai_new_app_dark", request = "", darkTheme = true)

    private fun assertScreenshot(
        name: String,
        request: String,
        changeCurrent: Boolean = false,
        canChangeCurrent: Boolean = true,
        darkTheme: Boolean = false
    ) {
        composeRule.setContent {
            AppTheme(darkTheme = darkTheme, dynamicColor = false) {
                Box(Modifier.fillMaxSize().testTag(ROOT_TAG)) {
                    AskAiScreen(
                        request = request,
                        onRequestChange = {},
                        canChangeCurrent = canChangeCurrent,
                        changeCurrent = changeCurrent,
                        onChangeCurrentChange = {},
                        tokens = 12_345,
                        onShare = {},
                        onCopy = {},
                        onBack = {}
                    )
                }
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
        val diffPixels = IntArray(actualPixels.size) { if (expectedPixels[it] != actualPixels[it]) DIFF_COLOR else actualPixels[it] }
        val changedPixels = expectedPixels.indices.count { expectedPixels[it] != actualPixels[it] }

        val outputDirectory = File("build/outputs/ask-ai-screenshots").apply { mkdirs() }
        if (changedPixels > 0) {
            writeBitmap(actual, File(outputDirectory, "$name.actual.png"))
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
        private const val ROOT_TAG = "ask-ai"
        private const val RECORD_ARGUMENT = "recordScreenshots"
        private const val DIFF_COLOR = 0xFFFF00FF.toInt()
    }
}
