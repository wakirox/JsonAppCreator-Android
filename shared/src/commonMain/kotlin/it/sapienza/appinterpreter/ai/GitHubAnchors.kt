package it.sapienza.appinterpreter.ai

/**
 * The anchors GitHub gives a Markdown document's headings, in order: lower case, punctuation removed, spaces as
 * hyphens, and -1, -2... after a repeated one. Code blocks are skipped.
 */
internal class GitHubAnchors private constructor(private val headings: List<Pair<String, String>>) {

    /** The anchor of the first heading [text] (without its #s), after the heading [after] when given. */
    fun of(text: String, after: String? = null): String? {
        val start =
            after?.let { name -> headings.indexOfFirst { it.first == name }.takeIf { it >= 0 } ?: return null } ?: -1
        return headings.drop(start + 1).firstOrNull { it.first == text }?.second
    }

    companion object {
        fun of(markdown: String): GitHubAnchors {
            val used = mutableMapOf<String, Int>()
            var inCode = false
            val headings = markdown.lines().mapNotNull { line ->
                if (line.startsWith("```")) inCode = !inCode
                val heading = HEADING.matchEntire(line)?.groupValues?.get(1)?.trim()
                if (inCode || heading == null) return@mapNotNull null
                val slug = slug(heading)
                val count = used[slug]
                used[slug] = (count ?: -1) + 1
                heading to if (count == null) slug else "$slug-${count + 1}"
            }
            return GitHubAnchors(headings)
        }

        fun slug(heading: String): String =
            heading.lowercase().filter { it.isLetterOrDigit() || it == ' ' || it == '-' || it == '_' }.replace(' ', '-')

        private val HEADING = Regex("#{1,6} (.+)")
    }
}
