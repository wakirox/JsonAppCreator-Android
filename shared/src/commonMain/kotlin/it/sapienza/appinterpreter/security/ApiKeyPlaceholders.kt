package it.sapienza.appinterpreter.security

object ApiKeyPlaceholders {
    private val placeholderPattern = Regex("__([A-Z][A-Z0-9_]*)__")

    /** The placeholder names in [text], sorted. */
    fun names(text: String): Set<String> = placeholderPattern.findAll(text).map { it.groupValues[1] }.toSortedSet()

    fun configurationId(json: String): String = "file:" + Sha256.digest(json.encodeToByteArray())
        .joinToString("") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }

    fun replace(text: String, values: Map<String, String>): String = placeholderPattern.replace(text) { match ->
        values[match.groupValues[1]] ?: match.value
    }

    private fun Sequence<String>.toSortedSet(): Set<String> = sorted().toCollection(LinkedHashSet())
}
