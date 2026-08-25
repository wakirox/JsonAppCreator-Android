package it.sapienza.appinterpreter.security

import java.security.MessageDigest

object ApiKeyPlaceholders {
    private val placeholderPattern = Regex("__([A-Z][A-Z0-9_]*)__")

    fun names(text: String): Set<String> =
        placeholderPattern.findAll(text).map { it.groupValues[1] }.toSortedSet()

    fun configurationId(json: String): String =
        "file:" + MessageDigest.getInstance("SHA-256")
            .digest(json.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    fun replace(text: String, values: Map<String, String>): String =
        placeholderPattern.replace(text) { match ->
            values[match.groupValues[1]] ?: match.value
        }
}
