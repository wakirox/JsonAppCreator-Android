package it.sapienza.appinterpreter.ai

import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Roughly how many tokens a text takes in an AI chat: about 3.8 characters each, which is what OpenAI's tokenizer
 * gives for the guide, schema and examples (3.7 to 4.1). Other AI apps count a little differently, so it is shown
 * rounded, as an estimate.
 */
object TokenEstimate {
    private const val CHARACTERS_PER_TOKEN = 3.8

    fun of(text: String): Int = ceil(text.length / CHARACTERS_PER_TOKEN).toInt()

    /** [tokens] to the nearest hundred, and at least 100: precise enough to read, not to rely on. */
    fun rounded(tokens: Int): Int = ((tokens / 100.0).roundToInt() * 100).coerceAtLeast(100)
}
