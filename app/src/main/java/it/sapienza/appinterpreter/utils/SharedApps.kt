package it.sapienza.appinterpreter.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import java.net.URI

/**
 * An app travels as its JSON file: that's all another phone needs to run it. API keys never travel, since the JSON
 * only holds their placeholders. What's here doesn't need Android, so it's tested directly.
 */
object SharedApps {
    /** How a received app relates to Your apps. */
    sealed interface Arrival {
        /** No saved app has its name. */
        data object New : Arrival
        /** The same app, unchanged: opening it changes nothing. */
        data object Unchanged : Arrival
        /** Another app, or another version, has the name: it's replaced, or both are kept under different names. */
        data class SameName(val saved: AppLibrary.SavedApp) : Arrival
    }

    fun arrival(json: String, saved: AppLibrary.SavedApp?): Arrival = when {
        saved == null -> Arrival.New
        sameJson(json, saved.json) -> Arrival.Unchanged
        else -> Arrival.SameName(saved)
    }

    /** "Shopping list.appkin.json": the app's name, safe as a file name everywhere it may be saved. */
    fun fileName(name: String): String {
        val safe = name.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), " ").replace(Regex("\\s+"), " ").trim().trim('.')
        return safe.take(MAX_NAME_LENGTH).trim().ifEmpty { "App" } + EXTENSION
    }

    /** The websites the app talks to or loads pictures from, such as api.themoviedb.org, in alphabetical order. */
    fun webAddresses(json: String): List<String> {
        val root = runCatching { mapper.readTree(json) }.getOrNull() ?: return emptyList()
        val hosts = sortedSetOf<String>()
        fun visit(node: JsonNode) {
            when {
                node.isTextual -> hostOf(node.asText())?.let(hosts::add)
                node.isContainerNode -> node.forEach(::visit)
            }
        }
        visit(root)
        return hosts.toList()
    }

    /** [json] with its name changed to [name], for keeping a received app next to a saved one with the same name. */
    fun renamed(json: String, name: String): String {
        val root = mapper.readTree(json) as ObjectNode
        root.put("name", name)
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root)
    }

    private fun hostOf(text: String): String? {
        val value = text.trim()
        if (!value.startsWith("https://", ignoreCase = true) && !value.startsWith("http://", ignoreCase = true)) return null
        // ${placeholders} in a path or query aren't valid URI characters, and only the host matters.
        val authority = value.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#')
        return runCatching { URI("https://$authority").host }.getOrNull()?.lowercase()?.takeIf { it.isNotEmpty() }
    }

    private fun sameJson(a: String, b: String) =
        runCatching { mapper.readTree(a) == mapper.readTree(b) }.getOrDefault(false)

    const val EXTENSION = ".appkin.json"
    private const val MAX_NAME_LENGTH = 60
    private val mapper = ObjectMapper()
}
