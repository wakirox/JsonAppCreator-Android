package it.sapienza.appinterpreter.ai

import kotlin.test.Test
import kotlin.test.assertEquals

class TokenEstimateTest {
    @Test
    fun aboutFourCharactersATokenRoundedUp() {
        assertEquals(0, TokenEstimate.of(""))
        assertEquals(1, TokenEstimate.of("abc"))
        assertEquals(2, TokenEstimate.of("abcde"))
        assertEquals(
            6072,
            TokenEstimate.of("x".repeat(23072)),
            "the prompt's length, which OpenAI's tokenizer counts as 6,020"
        )
    }

    @Test
    fun shownToTheNearestHundred() {
        assertEquals(6100, TokenEstimate.rounded(6072))
        assertEquals(6000, TokenEstimate.rounded(6049))
        assertEquals(100, TokenEstimate.rounded(12))
    }
}
