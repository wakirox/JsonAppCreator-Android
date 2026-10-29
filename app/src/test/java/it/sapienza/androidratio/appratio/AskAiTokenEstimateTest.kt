package it.sapienza.androidratio.appratio

import androidx.test.platform.app.InstrumentationRegistry
import it.sapienza.appinterpreter.ai.AskAi
import it.sapienza.appinterpreter.ai.TokenEstimate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What the AI dialog says the instructions take of the AI chat, built from the app's real guide, schema and examples. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AskAiTokenEstimateTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val currentApp = """{ "schemaVersion": 3, "name": "Big", "version": "1.0", "mainView": { "type": "text", "title": "${"x".repeat(
        38_000
    )}" } }"""

    @Test
    fun theInstructionsAloneTakeSomeThousandTokens() {
        val tokens = AskAi.tokens(context, null, null)
        assertTrue("$tokens", tokens in 4_000..12_000)
        assertEquals(TokenEstimate.of(AskAi.context(context, null, null)), tokens)
    }

    @Test
    fun theCurrentAppAndTheRequestAddTheirShare() {
        val instructions = AskAi.tokens(context, null, null)
        val withApp = AskAi.tokens(context, currentApp, null)
        assertTrue(
            "the current app adds about 10,000: ${withApp - instructions}",
            withApp - instructions in 9_500..10_500
        )
        val withRequest = AskAi.tokens(context, null, "A book list. ".repeat(60))
        assertTrue(
            "780 characters add about 200: ${withRequest - instructions}",
            withRequest - instructions in 150..250
        )
    }
}
