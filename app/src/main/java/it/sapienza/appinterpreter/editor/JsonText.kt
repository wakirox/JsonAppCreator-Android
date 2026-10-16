package it.sapienza.appinterpreter.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.core.util.Separators
import com.fasterxml.jackson.databind.ObjectMapper

/** [json] re-indented with two spaces, keys in their order and without escaped slashes; unchanged if it isn't valid JSON. */
fun prettyJson(json: String): String = runCatching {
    val indenter = DefaultIndenter("  ", "\n")
    val printer = DefaultPrettyPrinter()
        .withSeparators(Separators.createDefaultInstance().withObjectFieldValueSpacing(Separators.Spacing.AFTER))
        .withObjectIndenter(indenter)
        .withArrayIndenter(indenter)
    ObjectMapper().writer(printer).writeValueAsString(ObjectMapper().readTree(json))
}.getOrDefault(json)

private val JSON_TOKEN = Regex(""""(?:[^"\\\n]|\\.)*"?(\s*:)?|[{}\[\],]|-?\b\d+(?:\.\d+)?\b|\btrue\b|\bfalse\b|\bnull\b""")

/** Colours for JSON text: property names, strings, numbers and literals, and punctuation. */
data class JsonColors(val key: Color, val string: Color, val literal: Color, val punctuation: Color)

fun highlightJson(json: String, colors: JsonColors): AnnotatedString {
    val builder = AnnotatedString.Builder(json)
    JSON_TOKEN.findAll(json).forEach { match ->
        val colon = match.groupValues[1]
        val color = when {
            match.value.startsWith("\"") && colon.isNotEmpty() -> colors.key
            match.value.startsWith("\"") -> colors.string
            match.value[0] in "{}[]," -> colors.punctuation
            else -> colors.literal
        }
        builder.addStyle(SpanStyle(color = color), match.range.first, match.range.last + 1 - colon.length)
    }
    return builder.toAnnotatedString()
}

/** Syntax colouring for a text field; it changes no characters, so offsets map one to one. */
class JsonHighlighting(private val colors: JsonColors) : VisualTransformation {
    override fun filter(text: AnnotatedString) = TransformedText(highlightJson(text.text, colors), OffsetMapping.Identity)
}
