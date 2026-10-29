package it.sapienza.appinterpreter.runtime

/** The tags a tags field offers while the user types: ones already used elsewhere in its collection. */
object TagSuggestions {
    /** How many suggestions show at once. */
    const val MAX = 8

    /**
     * Of [known] tags, those not in [current] that match [typed] (a leading # and case aside): ones that start with it
     * first, then ones that contain it, each alphabetically. With nothing typed, all of them.
     */
    fun matching(known: List<String>, current: List<String>, typed: String): List<String> {
        val query = typed.trim().trimStart('#').lowercase()
        val unused = known.filter { tag ->
            current.none { it.equals(tag, ignoreCase = true) }
        }.sortedBy(String::lowercase)
        if (query.isEmpty()) return unused.take(MAX)
        val (starting, rest) = unused.partition { it.lowercase().startsWith(query) }
        return (
            starting + rest.filter {
                query in it.lowercase()
            }
            ).filterNot { it.equals(query, ignoreCase = true) }.take(MAX)
    }
}
