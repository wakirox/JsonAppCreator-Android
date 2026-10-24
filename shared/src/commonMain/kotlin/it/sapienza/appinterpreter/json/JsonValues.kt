package it.sapienza.appinterpreter.json

import it.sapienza.appinterpreter.security.UriEncoding
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The value at a dot path such as "owner.name". Array items are reached by index, as "meals.0.strMeal" or
 * "meals[0].strMeal". Null when a step is missing or the value is null.
 */
fun JsonObject.valueAt(mapping: String): JsonElement? {
    var current: JsonElement? = this
    for (step in mapping.replace("[", ".").replace("]", "").split('.').filter { it.isNotEmpty() }) {
        current = when (current) {
            is JsonObject -> current[step]
            is JsonArray -> step.toIntOrNull()?.let { current.getOrNull(it) }
            else -> null
        }
        if (current == null || current is JsonNull) return null
    }
    return current
}

/** The text at [mapping]: a value as shown on screen, an array's items joined with ", ", or null when missing. */
fun JsonObject.getValue(mapping: String): String? = when (val value = valueAt(mapping)) {
    null -> null
    is JsonArray -> value.joinToString(", ") { it.displayText() }
    else -> value.displayText()
}

fun JsonObject.getArray(mapping: String): JsonArray? = valueAt(mapping) as? JsonArray

/** The object at a path, such as the one item of a REST response with "meals.0". */
fun JsonObject.getObject(mapping: String): JsonObject? = valueAt(mapping) as? JsonObject

/** The text of the property [key], "" when it is missing (as org.json's optString). */
fun JsonObject.optString(key: String): String = this[key]?.displayText().orEmpty()

/** How a value reads on screen: text without quotes, numbers as the app always showed them, objects as JSON. */
fun JsonElement.displayText(): String = when (this) {
    is JsonNull -> "null"
    is JsonPrimitive -> if (isString || content == "true" || content == "false") content else JsonNumbers.display(content)
    else -> toString()
}

/** The value as a Kotlin string, boolean or null, for form fields; a list (a photos field's) as it is; others as text. */
fun JsonElement.toFormValue(): Any? = when (this) {
    is JsonNull -> null
    is JsonArray -> this
    is JsonPrimitive -> when {
        isString -> content
        content == "true" -> true
        content == "false" -> false
        else -> displayText()
    }
    else -> toString()
}

/** A form value (text, true/false, a number or null) as JSON. */
fun formValueToJson(value: Any?): JsonElement = when (value) {
    null -> JsonNull
    is Boolean -> JsonPrimitive(value)
    is Number -> JsonPrimitive(value)
    is JsonElement -> value
    else -> JsonPrimitive(value.toString())
}

private val PLACEHOLDER = Regex("\\$\\{([^}]+)\\}")

/** The text with each ${path} replaced by its value in [data]; a placeholder without a value becomes "". */
fun String.matchReplace(data: JsonObject?): String =
    PLACEHOLDER.replace(this) { match -> data?.getValue(match.groupValues[1]).orEmpty() }

/**
 * The text filled from [data] line by line, for text a person reads elsewhere (copied, say): a line whose placeholders
 * all lack a value is left out, as are the blank lines at either end.
 */
fun String.filledLines(data: JsonObject?): String = lines().mapNotNull { line ->
    val paths = PLACEHOLDER.findAll(line).map { it.groupValues[1] }.toList()
    if (paths.isNotEmpty() && paths.all { data?.getValue(it) == null }) null else line.matchReplace(data)
}.joinToString("\n").trim()

/** The paths of the placeholders in this text that have no value in [data], in order. */
fun String.missingPlaceholders(data: JsonObject?): List<String> =
    PLACEHOLDER.findAll(this).map { it.groupValues[1] }.filter { data?.getValue(it) == null }.distinct().toList()

/**
 * An address with its placeholders filled in from [data], or null when one of them has no value: such an
 * address would ask for something else, so it isn't used. Each value is percent-encoded ("arms & armor" becomes
 * "arms%20%26%20armor"), so it can't change the address around it; the template itself is left as written. A
 * placeholder that starts the address, as in "${'$'}{images.web.url}", supplies the address itself and isn't encoded.
 * A query parameter that is one placeholder, as "&type=${'$'}{type}", is an optional filter: without a value, or with
 * an empty one, it is left out (see [withoutEmptyFilters]).
 */
fun String.fillAddress(data: JsonObject?): String? {
    val template = withoutEmptyFilters(data)
    if (template.missingPlaceholders(data).isNotEmpty()) return null
    return PLACEHOLDER.replace(template) { match ->
        val value = data?.getValue(match.groupValues[1]).orEmpty()
        if (match.range.first == 0) value else UriEncoding.encode(value)
    }
}

/** The placeholders an address needs and [data] lacks, once its optional filters are left out. */
fun String.missingAddressPlaceholders(data: JsonObject?): List<String> = withoutEmptyFilters(data).missingPlaceholders(data)

private val FILTER = Regex("^[^=]+=\\$\\{([^}]+)\\}$")

/** This address without the query parameters "name=${'$'}{path}" whose path has no value, or an empty one, in [data]. */
fun String.withoutEmptyFilters(data: JsonObject?): String {
    val queryStart = indexOf('?').takeIf { it >= 0 } ?: return this
    val fragmentStart = indexOf('#', queryStart).takeIf { it >= 0 } ?: length
    val kept = substring(queryStart + 1, fragmentStart).split('&').filter { parameter ->
        val path = FILTER.find(parameter)?.groupValues?.get(1) ?: return@filter true
        !data?.getValue(path).isNullOrEmpty()
    }
    val query = if (kept.isEmpty()) "" else "?" + kept.joinToString("&")
    return substring(0, queryStart) + query + substring(fragmentStart)
}

/**
 * True when [data] has something at [path]: not missing, null, "", false, 0 or an empty array or object.
 * This is what "visibleIf" tests.
 */
fun JsonObject?.hasValueAt(path: String): Boolean = when (val value = this?.valueAt(path)) {
    null -> false
    is JsonArray -> value.isNotEmpty()
    is JsonObject -> value.isNotEmpty()
    is JsonPrimitive -> when {
        value.isString -> value.content.isNotBlank()
        value.content == "false" -> false
        else -> value.content.toDoubleOrNull()?.let { it != 0.0 } ?: true
    }
}
