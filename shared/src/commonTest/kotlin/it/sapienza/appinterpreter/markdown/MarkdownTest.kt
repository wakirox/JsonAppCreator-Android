package it.sapienza.appinterpreter.markdown

import it.sapienza.appinterpreter.markdown.Markdown.Block
import it.sapienza.appinterpreter.markdown.Markdown.Span
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownTest {

    @Test
    fun blocksAreHeadingsParagraphsListsQuotesAndCode() {
        val blocks = Markdown.parse(
            """
            # Budapest
            The pearl of
            the Danube.

            - Parliament
              by the river
            1. Bastion
            > Go early

            ```json
            { "a": 1 }
            ```
            """.trimIndent()
        )
        assertEquals(
            listOf(
                Block.Heading(1, listOf(Span("Budapest"))),
                Block.Paragraph(listOf(Span("The pearl of the Danube."))),
                Block.ListItem(listOf(Span("Parliament by the river")), number = null, depth = 0),
                Block.ListItem(listOf(Span("Bastion")), number = 1, depth = 0),
                Block.Quote(listOf(Span("Go early"))),
                Block.Code("{ \"a\": 1 }", "json")
            ),
            blocks
        )
    }

    @Test
    fun inlineBoldItalicsCodeAndLinks() {
        assertEquals(
            listOf(
                Span("Visit "),
                Span("the Parliament", bold = true),
                Span(", "),
                Span("early", italic = true),
                Span(", see "),
                Span("tickets", link = "https://example.com/t"),
                Span(" or "),
                Span("x_y", code = true)
            ),
            Markdown.inline("Visit **the Parliament**, *early*, see [tickets](https://example.com/t) or `x_y`")
        )
    }

    @Test
    fun bareAddressesAreLinksWithoutTheSentencesFullStop() {
        assertEquals(
            listOf(Span("See "), Span("https://example.com/a", link = "https://example.com/a"), Span(".")),
            Markdown.inline("See https://example.com/a.")
        )
    }

    @Test
    fun loneMarkersSnakeCaseAndEscapesStayAsWritten() {
        assertEquals(listOf(Span("5 * 3 = 15")), Markdown.inline("5 * 3 = 15"))
        assertEquals(listOf(Span("a snake_case_name")), Markdown.inline("a snake_case_name"))
        assertEquals(listOf(Span("*not italic*")), Markdown.inline("\\*not italic\\*"))
        assertEquals(listOf(Span("an `open tick")), Markdown.inline("an `open tick"))
    }

    @Test
    fun twoSpacesOrABackslashBreakALine() {
        assertEquals(listOf(Block.Paragraph(listOf(Span("one\ntwo\nthree")))), Markdown.parse("one  \ntwo\\\nthree"))
    }

    @Test
    fun nestedListItemsHaveADepth() {
        val items = Markdown.parse("- a\n  - b\n    - c").map { (it as Block.ListItem).depth }
        assertEquals(listOf(0, 1, 2), items)
    }
}
