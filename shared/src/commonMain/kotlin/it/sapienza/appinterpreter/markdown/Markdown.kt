package it.sapienza.appinterpreter.markdown

/**
 * The Markdown AI apps write, read into blocks for a screen to style: headings, paragraphs, bullet and numbered lists,
 * quotes and fenced code; inside them bold, italics, `code` and [links](https://…). Anything else (tables, images,
 * HTML) stays as written. Lines of a paragraph join with a space, as in CommonMark; a line ending in two spaces or a
 * backslash breaks it.
 */
object Markdown {

    /** A run of text with one style; [link] is the address it opens. */
    data class Span(
        val text: String,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val code: Boolean = false,
        val link: String? = null
    )

    sealed interface Block {
        /** A heading of [level] 1 (#) to 6 (######). */
        data class Heading(val level: Int, val spans: List<Span>) : Block
        data class Paragraph(val spans: List<Span>) : Block

        /** A list item, [depth] 0 at the margin; [number] for a numbered one, else a bullet. */
        data class ListItem(val spans: List<Span>, val number: Int?, val depth: Int) : Block
        data class Quote(val spans: List<Span>) : Block

        /** Fenced code, as written, with its [language] if the fence names one. */
        data class Code(val text: String, val language: String?) : Block
    }

    fun parse(markdown: String): List<Block> {
        val blocks = mutableListOf<Block>()
        val lines = markdown.replace("\r\n", "\n").split('\n')
        var paragraph = mutableListOf<String>()
        var item: Triple<MutableList<String>, Int?, Int>? = null
        var quote = mutableListOf<String>()

        fun flush() {
            if (paragraph.isNotEmpty()) blocks += Block.Paragraph(inline(joined(paragraph)))
            item?.let { (text, number, depth) -> blocks += Block.ListItem(inline(joined(text)), number, depth) }
            if (quote.isNotEmpty()) blocks += Block.Quote(inline(joined(quote)))
            paragraph = mutableListOf()
            item = null
            quote = mutableListOf()
        }

        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            val fence = FENCE.matchEntire(line)
            when {
                fence != null -> {
                    flush()
                    val marker = fence.groupValues[1]
                    val code = mutableListOf<String>()
                    index++
                    while (index < lines.size && !lines[index].trimStart().startsWith(marker)) code += lines[index++]
                    blocks += Block.Code(code.joinToString("\n"), fence.groupValues[2].trim().ifEmpty { null })
                }

                line.isBlank() -> flush()

                HEADING.matchEntire(line) != null -> {
                    flush()
                    val heading = HEADING.matchEntire(line)!!
                    blocks +=
                        Block.Heading(heading.groupValues[1].length, inline(heading.groupValues[2].trimEnd('#', ' ')))
                }

                LIST_ITEM.matchEntire(line) != null -> {
                    flush()
                    val match = LIST_ITEM.matchEntire(line)!!
                    item =
                        Triple(
                            mutableListOf(match.groupValues[4]),
                            match.groupValues[3].toIntOrNull(),
                            match.groupValues[1].length / 2
                        )
                }

                line.startsWith(">") -> {
                    if (quote.isEmpty()) flush()
                    quote += line.removePrefix(">").removePrefix(" ")
                }

                else -> when {
                    item != null -> item!!.first += line.trim()
                    quote.isNotEmpty() -> quote += line.trim()
                    else -> paragraph += line
                }
            }
            index++
        }
        flush()
        return blocks
    }

    /** A block's lines as one text: joined with a space, or a line break after two spaces or a backslash. */
    private fun joined(lines: List<String>): String = buildString {
        lines.forEachIndexed { index, line ->
            val last = index == lines.lastIndex
            when {
                last -> append(line.trim())
                line.endsWith("  ") || line.endsWith("\\") -> append(line.trim().removeSuffix("\\")).append('\n')
                else -> append(line.trim()).append(' ')
            }
        }
    }

    /** The spans of one block's text: code first (nothing inside it is read), then links, bold and italics. */
    fun inline(text: String): List<Span> {
        val spans = mutableListOf<Span>()
        val plain = StringBuilder()
        var bold = false
        var italic = false

        fun flush() {
            if (plain.isNotEmpty()) spans += Span(plain.toString(), bold, italic)
            plain.clear()
        }

        var i = 0
        while (i < text.length) {
            val c = text[i]
            val rest = text.substring(i)
            when {
                c == '\\' && i + 1 < text.length && text[i + 1] in ESCAPABLE -> {
                    plain.append(text[i + 1])
                    i += 2
                    continue
                }

                c == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i) {
                        flush()
                        spans += Span(text.substring(i + 1, end), bold, italic, code = true)
                        i = end + 1
                        continue
                    }
                }

                c == '[' -> {
                    val link = LINK.matchAt(text, i)
                    if (link != null) {
                        flush()
                        inline(link.groupValues[1]).forEach {
                            spans +=
                                it.copy(
                                    bold = it.bold || bold,
                                    italic = it.italic || italic,
                                    link = link.groupValues[2]
                                )
                        }
                        i += link.value.length
                        continue
                    }
                }

                (
                    rest.startsWith(
                        "**"
                    ) || rest.startsWith("__")
                    ) && (bold || closes(text, i + 2, rest.substring(0, 2))) &&
                    (rest.startsWith("**") || boundaryBefore(text, i)) -> {
                    flush()
                    bold = !bold
                    i += 2
                    continue
                }

                (c == '*' || c == '_') && (italic || closes(text, i + 1, c.toString())) &&
                    (c == '*' || boundaryBefore(text, i)) &&
                    (italic || (i + 1 < text.length && !text[i + 1].isWhitespace())) -> {
                    flush()
                    italic = !italic
                    i += 1
                    continue
                }
            }
            plain.append(c)
            i++
        }
        flush()
        return spans.flatMap(::withBareLinks)
    }

    /** Whether [marker] appears again after [from], so an opening one has a partner (a lone * stays a star). */
    private fun closes(text: String, from: Int, marker: String) = text.indexOf(marker, from).let { it > from }

    /** An underscore opens or closes emphasis only between words, so snake_case stays as it is. */
    private fun boundaryBefore(text: String, index: Int) = index == 0 || !text[index - 1].isLetterOrDigit()

    /** [span] with each bare web address in it (outside links and code) a link of its own. */
    private fun withBareLinks(span: Span): List<Span> {
        if (span.link != null || span.code) return listOf(span)
        val parts = mutableListOf<Span>()
        var position = 0
        URL.findAll(span.text).forEach { match ->
            val url = match.value.trimEnd { it in TRAILING_PUNCTUATION }
            val end = match.range.first + url.length
            if (match.range.first >
                position
            ) {
                parts += span.copy(text = span.text.substring(position, match.range.first))
            }
            parts += span.copy(text = url, link = url)
            position = end
        }
        if (position < span.text.length) parts += span.copy(text = span.text.substring(position))
        return parts
    }

    private val FENCE = Regex("""\s{0,3}(```+|~~~+)(.*)""")
    private val HEADING = Regex("""\s{0,3}(#{1,6})\s+(.*)""")
    private val LIST_ITEM = Regex("""(\s*)([-*+]|(\d{1,9})[.)])\s+(.*)""")
    private val LINK = Regex("""\[([^\]]+)]\((\S+?)(?:\s+"[^"]*")?\)""")
    private val URL = Regex("""https?://[^\s<>"]+""")
    private const val ESCAPABLE = "\\`*_{}[]()#+-.!>|~"
    private const val TRAILING_PUNCTUATION = ".,;:!?)]}'"
}
