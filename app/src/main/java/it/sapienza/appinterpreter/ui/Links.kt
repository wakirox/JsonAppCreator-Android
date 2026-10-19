package it.sapienza.appinterpreter.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink

private val urlPattern = Regex("""https?://[^\s<>"]+""")

/** Punctuation that ends a sentence rather than a URL, as in "see https://example.com." */
private const val TRAILING_PUNCTUATION = ".,;:!?)]}'"

/** The web addresses in [text], as start and end offsets. */
internal fun urlRanges(text: String): List<IntRange> = urlPattern.findAll(text).mapNotNull { match ->
    val url = match.value.trimEnd { it in TRAILING_PUNCTUATION }
    if (url.substringAfter("://").isEmpty()) null else match.range.first until match.range.first + url.length
}.toList()

/** [text] with each web address in it a link that opens in the browser. */
internal fun linkified(text: String, styles: TextLinkStyles): AnnotatedString = buildAnnotatedString {
    var position = 0
    urlRanges(text).forEach { range ->
        append(text.substring(position, range.first))
        val url = text.substring(range.first, range.last + 1)
        withLink(LinkAnnotation.Url(url, styles)) { append(url) }
        position = range.last + 1
    }
    append(text.substring(position))
}
