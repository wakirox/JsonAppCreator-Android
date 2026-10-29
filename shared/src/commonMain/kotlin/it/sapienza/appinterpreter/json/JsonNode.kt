package it.sapienza.appinterpreter.json

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * A read-only walk through a JSON tree with Jackson's JsonNode semantics, which the schema-driven editor and AI code
 * were written against: [path] never fails (a missing node is [isMissing]), [asText] of a missing or null node gives
 * its default, iterating an object gives its values.
 */
class JsonNode(val element: JsonElement?) : Iterable<JsonNode> {
    val isMissing: Boolean get() = element == null
    val isNull: Boolean get() = element is JsonNull
    val isObject: Boolean get() = element is JsonObject
    val isArray: Boolean get() = element is JsonArray
    val isTextual: Boolean get() = element is JsonPrimitive && element.isString
    val isValueNode: Boolean get() = element is JsonPrimitive
    val isNumber: Boolean
        get() = element is JsonPrimitive && !element.isString && element !is JsonNull &&
            element.content != "true" && element.content != "false"

    fun path(name: String): JsonNode = JsonNode((element as? JsonObject)?.get(name))

    fun has(name: String): Boolean = (element as? JsonObject)?.containsKey(name) == true

    /** The text of a value; "" for a container, [default] for a missing or null node. */
    fun asText(default: String): String = text() ?: default

    /** As [asText], but null for a missing or null node (Jackson's asText(null)). */
    @kotlin.jvm.JvmName("asTextOrNull")
    fun asText(@Suppress("UNUSED_PARAMETER") default: Nothing?): String? = text()

    fun asText(): String = asText("")

    private fun text(): String? = when (element) {
        null, is JsonNull -> null
        is JsonPrimitive -> element.content
        else -> ""
    }

    /** An object's properties in order. */
    fun fields(): List<Pair<String, JsonNode>> = (element as? JsonObject)?.map { (key, value) ->
        key to JsonNode(value)
    }.orEmpty()

    override fun iterator(): Iterator<JsonNode> = when (element) {
        is JsonArray -> element.map(::JsonNode).iterator()
        is JsonObject -> element.values.map(::JsonNode).iterator()
        else -> emptyList<JsonNode>().iterator()
    }

    /** The node as compact JSON, as Jackson's toString(); "" for a missing node. */
    override fun toString(): String = element?.toString().orEmpty()

    override fun equals(other: Any?): Boolean = other is JsonNode && element == other.element
    override fun hashCode(): Int = element.hashCode()

    companion object {
        fun parse(json: String): JsonNode = JsonNode(JsonText.parse(json))
    }
}

/**
 * Whether two trees are equal as Jackson compared them: objects and arrays by their members, whole numbers by value,
 * decimals as doubles (1.0 equals 1.00), but never a whole number with a decimal (1 is not 1.0).
 */
fun JsonText.sameValue(a: JsonElement?, b: JsonElement?): Boolean = when {
    a is JsonObject && b is JsonObject -> a.keys == b.keys && a.all { (key, value) -> sameValue(value, b[key]) }

    a is JsonArray && b is JsonArray -> a.size == b.size && a.indices.all { sameValue(a[it], b[it]) }

    a is JsonNull || b is JsonNull -> a == b

    a is JsonPrimitive && b is JsonPrimitive -> when {
        a.isString || b.isString -> a == b
        a.content in BOOLEANS || b.content in BOOLEANS -> a.content == b.content
        JsonNumbers.isDecimal(a.content) != JsonNumbers.isDecimal(b.content) -> false
        JsonNumbers.isDecimal(a.content) -> a.content.toDouble() == b.content.toDouble()
        else -> JsonNumbers.exact(a.content) == JsonNumbers.exact(b.content)
    }

    else -> a == b
}

private val BOOLEANS = setOf("true", "false")
