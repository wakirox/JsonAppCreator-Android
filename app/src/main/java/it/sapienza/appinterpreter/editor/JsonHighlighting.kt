package it.sapienza.appinterpreter.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

// Possessive (*+, ++), so a very long string can't make the regex engine recurse until the stack overflows.
private val JSON_TOKEN =
    Regex(
        """"(?:[^"\\\n]++|\\.)*+"?(\s*:)?|[{}\[\],:]|-?\b\d+(?:\.\d+)?(?:[eE][+-]?\d+)?\b|\btrue\b|\bfalse\b|\bnull\b"""
    )

/** The editor's syntax colours, the same whatever the wallpaper, so each kind of token is easy to tell apart. */
data class JsonColors(
    val key: Color,
    val string: Color,
    val number: Color,
    val keyword: Color,
    val punctuation: Color,
    val problem: Color
) {
    companion object {
        val Light = JsonColors(
            key = Color(0xFF1A5FB4),
            string = Color(0xFF2E7D32),
            number = Color(0xFFC25E00),
            keyword = Color(0xFF8E24AA),
            punctuation = Color(0xFF6B7280),
            problem = Color(0x33E53935)
        )
        val Dark = JsonColors(
            key = Color(0xFF8AB4F8),
            string = Color(0xFFA5D6A7),
            number = Color(0xFFFFB74D),
            keyword = Color(0xFFCE93D8),
            punctuation = Color(0xFF9AA0A6),
            problem = Color(0x55EF5350)
        )
    }
}

/** [json] with syntax colours, and a red tint on the token at each of [problems] (character offsets). */
fun highlightJson(json: String, colors: JsonColors, problems: List<Int> = emptyList()): AnnotatedString {
    val builder = AnnotatedString.Builder(json)
    JSON_TOKEN.findAll(json).forEach { match ->
        val colon = match.groupValues[1]
        val value = match.value
        val color = when {
            value.startsWith("\"") && colon.isNotEmpty() -> colors.key
            value.startsWith("\"") -> colors.string
            value[0] in "{}[],:" -> colors.punctuation
            value[0] == '-' || value[0].isDigit() -> colors.number
            else -> colors.keyword
        }
        builder.addStyle(SpanStyle(color = color), match.range.first, match.range.last + 1 - colon.length)
    }
    problems.forEach { offset ->
        if (offset !in json.indices) return@forEach
        val token = JSON_TOKEN.find(json, offset)?.takeIf { it.range.first == offset }
        val end = token?.let { it.range.last + 1 - it.groupValues[1].length } ?: (offset + 1)
        builder.addStyle(SpanStyle(background = colors.problem), offset, end)
    }
    return builder.toAnnotatedString()
}

/** Syntax colouring for a text field; it changes no characters, so offsets map one to one. */
class JsonHighlighting(private val colors: JsonColors, private val problems: List<Int>) : VisualTransformation {
    override fun filter(text: AnnotatedString) =
        TransformedText(highlightJson(text.text, colors, problems), OffsetMapping.Identity)

    override fun equals(other: Any?) = other is JsonHighlighting && other.colors == colors && other.problems == problems
    override fun hashCode() = 31 * colors.hashCode() + problems.hashCode()
}
