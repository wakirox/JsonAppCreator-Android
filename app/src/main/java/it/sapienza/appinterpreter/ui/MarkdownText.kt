package it.sapienza.appinterpreter.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import it.sapienza.appinterpreter.markdown.Markdown
import it.sapienza.appinterpreter.markdown.Markdown.Block

/**
 * [blocks] (see [Markdown]) as one styled text, the way the HTML format looks: headings bold and larger, list items
 * with a bullet or their number, quotes in italics, code in a monospaced font on [codeBackground], links opening in
 * the browser. Blocks are a blank line apart, list items one line.
 */
internal fun markdownText(blocks: List<Block>, links: TextLinkStyles, codeBackground: Color): AnnotatedString = buildAnnotatedString {
    blocks.forEachIndexed { index, block ->
        if (index > 0) append(if (block is Block.ListItem && blocks[index - 1] is Block.ListItem) "\n" else "\n\n")
        when (block) {
            is Block.Heading -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = HEADING_SIZES.getOrElse(block.level - 1) { 1.em })) {
                spans(block.spans, links, codeBackground)
            }
            is Block.Paragraph -> spans(block.spans, links, codeBackground)
            is Block.ListItem -> {
                append("    ".repeat(block.depth))
                append(block.number?.let { "$it.  " } ?: "•  ")
                spans(block.spans, links, codeBackground)
            }
            is Block.Quote -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { spans(block.spans, links, codeBackground) }
            is Block.Code -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)) { append(block.text) }
        }
    }
}

/** Markdown [text] as styled text (see the other [markdownText]). */
internal fun markdownText(text: String, links: TextLinkStyles, codeBackground: Color): AnnotatedString =
    markdownText(Markdown.parse(text), links, codeBackground)

private fun AnnotatedString.Builder.spans(spans: List<Markdown.Span>, links: TextLinkStyles, codeBackground: Color) {
    spans.forEach { span ->
        val style = SpanStyle(
            fontWeight = if (span.bold) FontWeight.Bold else null,
            fontStyle = if (span.italic) FontStyle.Italic else null,
            fontFamily = if (span.code) FontFamily.Monospace else null,
            background = if (span.code) codeBackground else Color.Unspecified
        )
        val styled = span.bold || span.italic || span.code
        val write = { if (styled) withStyle(style) { append(span.text) } else append(span.text) }
        val link = span.link
        if (link != null) withLink(LinkAnnotation.Url(link, links)) { write() } else write()
    }
}

private val HEADING_SIZES = listOf(1.4.em, 1.25.em, 1.1.em)
