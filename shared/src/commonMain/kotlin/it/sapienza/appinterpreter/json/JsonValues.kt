package it.sapienza.appinterpreter.json

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

/** The value as a Kotlin string, boolean or null, for form fields; other values as their text. */
fun JsonElement.toFormValue(): Any? = when (this) {
    is JsonNull -> null
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

private val PLACEHOLDER = Regex("(\\$\\{[^}]+\\})")

/** The text with each ${path} replaced by its value in [data]; placeholders without a value stay as they are. */
fun String.matchReplace(data: JsonObject?): String {
    if (data == null) return this
    var result = this
    PLACEHOLDER.findAll(this).forEach { match ->
        val placeholder = match.groupValues[1]
        data.getValue(placeholder.substring(2, placeholder.length - 1))?.let { value ->
            result = result.replace(placeholder, value)
        }
    }
    return result
}
