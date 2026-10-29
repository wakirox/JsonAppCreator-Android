package it.sapienza.appinterpreter.runtime.usecase

import it.sapienza.appinterpreter.json.filledLines
import it.sapienza.appinterpreter.json.matchReplace
import it.sapienza.appinterpreter.json.valueAt
import it.sapienza.appinterpreter.markdown.Markdown
import it.sapienza.appinterpreter.model.event.ItemsExport
import it.sapienza.appinterpreter.model.event.PdfItems
import it.sapienza.appinterpreter.storage.Files
import it.sapienza.appinterpreter.storage.Photos
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * The web page a [PdfItems] event prints: its title, then each object's text read as Markdown, with the object's photos
 * below it. Every value is escaped, so saved text can't add HTML. A kept photo stays a reference ("appkin-photo:…") in
 * its img's src, for the phone to swap for the picture; a web address is loaded as it is.
 */
class PdfPageUseCase {
    /**
     * The page to print: [title] names the file, [photos] are the kept photos the [html] refers to. A [pageless] one
     * is one page as tall as its <main>, which is [CONTENT_WIDTH_MM] wide, plus [MARGIN_MM] above and below.
     */
    class Page(val title: String, val html: String, val photos: List<String>, val pageless: Boolean = false)

    operator fun invoke(event: ItemsExport, items: List<JsonObject>, data: JsonObject?, fallbackTitle: String): Page {
        val title = event.title?.matchReplace(data)?.trim()?.takeIf { it.isNotEmpty() } ?: fallbackTitle
        val photos = mutableListOf<String>()
        val body = StringBuilder("<h1>${escape(title)}</h1>\n")
        if (items.isEmpty()) {
            event.emptyText?.matchReplace(data)?.let { body.append("<p class=\"empty\">${escape(it)}</p>\n") }
        }
        items.forEach { item ->
            body.append("<section>\n").append(html(Markdown.parse(event.text.filledLines(item))))
            val pictures = event.photos?.let { photosAt(item, it) }.orEmpty()
            if (pictures.isNotEmpty()) {
                body.append("<div class=\"photos\">")
                pictures.forEach { body.append("<img src=\"${escape(it)}\">") }
                body.append("</div>\n")
                photos += pictures.filter(Photos::isPhoto)
            }
            val attached = event.files?.let { Files.of(item.valueAt(it)) }.orEmpty()
            if (attached.isNotEmpty()) {
                body.append("<ul class=\"files\">")
                attached.forEach { file -> body.append("<li>📎 ${escape(file.name)}${file.size?.let { " · " + Files.sizeText(it) }.orEmpty()}</li>") }
                body.append("</ul>\n")
            }
            body.append("</section>\n")
        }
        val html = "<!DOCTYPE html>\n<html><head><meta charset=\"utf-8\"><title>${escape(title)}</title>" +
            "<style>$STYLE</style></head>\n<body><main>\n$body</main></body></html>\n"
        return Page(title, html, photos.distinct(), (event as? PdfItems)?.pageless == true)
    }


    /** [blocks] as HTML; list items in a row share one list, a deeper one indented. */
    private fun html(blocks: List<Markdown.Block>): String = buildString {
        var openList: String? = null
        fun closeList() {
            openList?.let { append("</$it>\n") }
            openList = null
        }
        blocks.forEach { block ->
            if (block is Markdown.Block.ListItem) {
                val tag = if (block.number != null) "ol" else "ul"
                if (openList != tag) {
                    closeList()
                    append("<$tag>\n")
                    openList = tag
                }
                val indent = if (block.depth > 0) " style=\"margin-left:${block.depth * 1.5}em\"" else ""
                val value = block.number?.let { " value=\"$it\"" }.orEmpty()
                append("<li$value$indent>${spans(block.spans)}</li>\n")
                return@forEach
            }
            closeList()
            when (block) {
                // The page's own title is the h1: an object's headings start one level below it.
                is Markdown.Block.Heading -> (block.level + 1).coerceAtMost(6).let { append("<h$it>${spans(block.spans)}</h$it>\n") }
                is Markdown.Block.Paragraph -> append("<p>${spans(block.spans)}</p>\n")
                is Markdown.Block.Quote -> append("<blockquote>${spans(block.spans)}</blockquote>\n")
                is Markdown.Block.Code -> append("<pre>${escape(block.text)}</pre>\n")
                is Markdown.Block.ListItem -> Unit
            }
        }
        closeList()
    }

    private fun spans(spans: List<Markdown.Span>): String = spans.joinToString("") { span ->
        var text = escape(span.text)
        if (span.code) text = "<code>$text</code>"
        if (span.italic) text = "<em>$text</em>"
        if (span.bold) text = "<strong>$text</strong>"
        span.link?.takeIf { LINK_SCHEMES.any(it::startsWith) }?.let { text = "<a href=\"${escape(it)}\">$text</a>" }
        text
    }

    companion object {
        /** An object's photos at [path] (a list, or one): kept ones and web addresses. */
        fun photosAt(item: JsonObject, path: String): List<String> = when (val value = item.valueAt(path)) {
            is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            is JsonPrimitive -> listOfNotNull(value.contentOrNull)
            else -> emptyList()
        }.filter { Photos.isPhoto(it) || it.startsWith("https://") }

        private val LINK_SCHEMES = listOf("https://", "http://", "mailto:")

        /** An A4 page's width less its side margins: the width the content is laid out at. */
        const val CONTENT_WIDTH_MM = 182

        /** The margin above and below the content, and on each side ([CONTENT_WIDTH_MM] leaves 14 mm). */
        const val MARGIN_MM = 16

        fun escape(text: String): String =
            text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

        /**
         * A plain, readable page: A4 margins. An object may go on over a page, but its heading stays with what follows
         * and no photo or paragraph is cut.
         */
        private val STYLE = """
            @page { margin: ${MARGIN_MM}mm 14mm; }
            main { width: ${CONTENT_WIDTH_MM}mm; }
            body { font-family: sans-serif; color: #1d1b20; font-size: 11pt; line-height: 1.45; }
            h1 { font-size: 20pt; margin: 0 0 6mm; }
            h2, h3 { font-size: 14pt; margin: 0 0 1.5mm; break-after: avoid; }
            h4, h5, h6 { font-size: 12pt; margin: 0 0 1mm; }
            p, ul, ol, blockquote, pre { margin: 0 0 2mm; }
            section { padding: 0 0 4mm; margin: 0 0 4mm; border-bottom: 0.3mm solid #ddd; }
            p, li, img { break-inside: avoid; }
            section:last-child { border-bottom: none; }
            blockquote { border-left: 1mm solid #ccc; padding-left: 3mm; color: #555; }
            code, pre { font-family: monospace; background: #f3f3f3; }
            pre { padding: 2mm; white-space: pre-wrap; }
            a { color: #2a5bd7; }
            .photos { display: flex; flex-wrap: wrap; align-items: flex-start; gap: 2mm; margin-top: 2mm; }
            .photos img { max-height: 110mm; max-width: 100%; object-fit: contain; border-radius: 1.5mm; }
            .empty { color: #666; }
            .files { list-style: none; padding: 0; margin: 2mm 0 0; color: #444; }
        """.trimIndent().replace("\n", " ")
    }
}
