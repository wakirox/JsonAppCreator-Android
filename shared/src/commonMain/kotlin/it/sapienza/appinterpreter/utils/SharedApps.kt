package it.sapienza.appinterpreter.utils

import it.sapienza.appinterpreter.json.JsonText
import it.sapienza.appinterpreter.json.sameValue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * An app travels as its JSON file: that's all another phone needs to run it. API keys never travel, since the JSON
 * only holds their placeholders. What's here is common code, so it's tested without a phone.
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
        val safe = name.replace(UNSAFE_IN_FILE_NAMES, " ").replace(Regex("\\s+"), " ").trim().trim('.')
        return safe.take(MAX_NAME_LENGTH).trim().ifEmpty { "App" } + EXTENSION
    }

    /** The websites the app talks to or loads pictures from, such as api.themoviedb.org, in alphabetical order. */
    fun webAddresses(json: String): List<String> {
        val root = runCatching { JsonText.parse(json) }.getOrNull() ?: return emptyList()
        val hosts = mutableSetOf<String>()
        fun visit(element: JsonElement) {
            when (element) {
                is JsonObject -> element.values.forEach(::visit)
                is JsonArray -> element.forEach(::visit)
                is JsonPrimitive -> if (element.isString) hostOf(element.content)?.let(hosts::add)
            }
        }
        visit(root)
        return hosts.sorted()
    }

    /** [json] with its name changed to [name], for keeping a received app next to a saved one with the same name. */
    fun renamed(json: String, name: String): String {
        val root = JsonText.parse(json) as JsonObject
        // The name keeps its place (a missing one goes last). Written as the editor formats JSON, numbers as they were.
        return JsonText.pretty(JsonObject(root + ("name" to JsonPrimitive(name))))
    }

    /** The host of a web address, lowercased, or null when [text] isn't one (as java.net.URI read it). */
    internal fun hostOf(text: String): String? {
        val value = text.trim()
        if (!value.startsWith("https://", ignoreCase = true) && !value.startsWith("http://", ignoreCase = true)) return null
        // ${placeholders} in a path or query aren't valid URI characters, and only the host matters.
        val authority = value.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#')
        if (authority.any { it.isWhitespace() || it in NOT_IN_AUTHORITY || it.code < 0x20 || it.code == 0x7F }) return null
        val hostAndPort = authority.substringAfterLast('@')
        val host: String
        val port: String
        if (hostAndPort.startsWith('[')) {
            if (']' !in hostAndPort) return null
            host = hostAndPort.substringBefore(']') + "]"
            val rest = hostAndPort.substringAfter(']')
            if (rest.isNotEmpty() && !rest.startsWith(':')) return null
            port = rest.removePrefix(":")
        } else {
            host = hostAndPort.substringBefore(':')
            port = hostAndPort.substringAfter(':', "")
        }
        if (!port.all { it.isDigit() }) return null
        val valid = host.matches(HOST_NAME) || host.matches(IPV4) || (host.startsWith('[') && host.matches(IPV6))
        return host.takeIf { valid && it.isNotEmpty() }?.lowercase()
    }

    private fun sameJson(a: String, b: String) =
        runCatching { JsonText.sameValue(JsonText.parse(a), JsonText.parse(b)) }.getOrDefault(false)

    const val EXTENSION = ".appkin.json"
    private const val MAX_NAME_LENGTH = 60
    private val UNSAFE_IN_FILE_NAMES = Regex("[\\\\/:*?\"<>|\\u0000-\\u001F\\u007F]")
    private const val NOT_IN_AUTHORITY = "\"<>{}|\\^`"
    private val HOST_NAME = Regex("([A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?\\.)*[A-Za-z]([A-Za-z0-9-]*[A-Za-z0-9])?\\.?")
    private val IPV4 = Regex("\\d{1,3}(\\.\\d{1,3}){3}")
    private val IPV6 = Regex("\\[[0-9A-Fa-f:.]+]")
}
