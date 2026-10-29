package it.sapienza.appinterpreter.storage

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Files attached with a files field: each a link to a file on the phone (not a copy), saved as an object with what
 * shows it when the file can't be read: { "name": "plan.pdf", "type": "application/pdf", "size": 2048, "uri": "content://…" }.
 */
object Files {
    /** How many files a files field takes when it doesn't say. */
    const val DEFAULT_MAX = 10

    /** One attached file: its [uri] is the phone's link to it. */
    data class Attached(val name: String, val type: String?, val size: Long?, val uri: String) {
        fun toJson(): JsonObject = JsonObject(
            listOfNotNull(
                "name" to JsonPrimitive(name),
                type?.let { "type" to JsonPrimitive(it) },
                size?.let { "size" to JsonPrimitive(it) },
                "uri" to JsonPrimitive(uri)
            ).toMap()
        )
    }

    /** The files in a files field's value (a list of objects; one object counts too); anything else is none. */
    fun of(value: JsonElement?): List<Attached> = when (value) {
        is JsonArray -> value.mapNotNull(::attached)
        is JsonObject -> listOfNotNull(attached(value))
        else -> emptyList()
    }

    private fun attached(element: JsonElement): Attached? {
        val obj = element as? JsonObject ?: return null
        val uri = (obj["uri"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() } ?: return null
        val name = (obj["name"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() } ?: uri.substringAfterLast('/')
        return Attached(name, (obj["type"] as? JsonPrimitive)?.contentOrNull, (obj["size"] as? JsonPrimitive)?.longOrNull, uri)
    }

    /** A size as people read it: "820 B", "12 KB", "2.4 MB". */
    fun sizeText(bytes: Long): String = when {
        bytes < 1_000 -> "$bytes B"
        bytes < 1_000_000 -> "${(bytes + 500) / 1_000} KB"
        else -> {
            val tenths = (bytes + 50_000) / 100_000
            "${tenths / 10}.${tenths % 10} MB"
        }
    }

    /** A file name safe in a zip and a Markdown link: letters, digits, dots, hyphens and underscores. */
    fun safeName(name: String): String =
        name.map { if (it.isLetterOrDigit() || it in "._-") it else '-' }.joinToString("").replace(Regex("-+"), "-").trim('-', '.')
            .ifEmpty { "file" }
}
