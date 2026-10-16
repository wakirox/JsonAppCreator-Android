package it.sapienza.appinterpreter.editor

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * A suggestion for the cursor: replace [start] until [end] with [insert], then put the cursor [cursor] characters into
 * the inserted text.
 */
data class Suggestion(val label: String, val start: Int, val end: Int, val insert: String, val cursor: Int)

/**
 * Autocomplete from the v3 JSON Schema: the property names an object may still have, and the allowed values of the
 * property being written. Views, form fields and events are told apart by their "type", as the schema does.
 */
class SchemaSuggestions(schemaJson: String) {
    private val root: JsonNode = ObjectMapper().readTree(schemaJson)
    private val defs: JsonNode = root.path("\$defs")

    fun at(text: String, cursor: Int, limit: Int = 12): List<Suggestion> {
        val at = JsonCursor.at(text, cursor)
        if (at.position == CursorPosition.NONE) return emptyList()
        // The object facts are read without the token being typed, so a half-written string doesn't confuse them.
        val facts = text.removeRange(at.replaceStart, at.replaceEnd).let { clean ->
            at.containers.map { if (it.isObject) JsonCursor.objectFacts(clean, it.start) else emptySet<String>() to null }
        }

        var schema: JsonNode? = root
        for (i in 1 until at.containers.size) {
            schema = schema?.let { child(it, facts[i - 1], at.containers[i].slot) }
        }
        schema ?: return emptyList()
        val container = at.containers.last()
        val (present, _) = facts.last()

        val candidates: List<Pair<String, String>> = when (at.position) {
            CursorPosition.KEY -> keySuggestions(schema, facts.last())
                .filter { (name, _) -> name !in present }
                .map { (name, value) -> name to "\"$name\": ${template(value)}" }
            CursorPosition.VALUE -> {
                val valueSchema = if (container.isObject) at.key?.let { properties(schema, facts.last())[it] } else items(schema)
                valueSchema?.let { values(it) }.orEmpty().map { it to literal(it, valueSchema!!) }
            }
            CursorPosition.NONE -> emptyList()
        }
        return candidates
            .filter { (label, _) -> label.startsWith(at.prefix, ignoreCase = true) && label != at.prefix }
            .take(limit)
            .map { (label, insert) ->
                // Inside an empty string, object or array value; otherwise after the insertion.
                val inside = if (insert.endsWith("\"\"") || insert.endsWith("{}") || insert.endsWith("[]")) {
                    insert.length - 1
                } else insert.length
                Suggestion(label, at.replaceStart, at.replaceEnd, insert, inside)
            }
    }

    private fun resolve(node: JsonNode): JsonNode {
        var current = node
        repeat(16) {
            val ref = current.path("\$ref").asText("")
            if (!ref.startsWith("#/\$defs/")) return current
            current = defs.path(ref.removePrefix("#/\$defs/"))
        }
        return current
    }

    /** The properties an object may have, given the keys it already has and its "type". */
    private fun properties(node: JsonNode, facts: Pair<Set<String>, String?>): Map<String, JsonNode> {
        val schema = resolve(node)
        val result = LinkedHashMap<String, JsonNode>()
        if (schema.has("if")) {
            val required = schema.path("if").path("required").map { it.asText() }
            val branch = if (facts.first.containsAll(required)) schema.path("then") else schema.path("else")
            result += properties(branch, facts)
        }
        schema.path("properties").fields().forEach { (name, value) -> result[name] = value }
        schema.path("allOf").forEach { part ->
            if (part.has("if")) {
                val type = part.path("if").path("properties").path("type").path("const").asText(null)
                if (type != null && type == facts.second) result += properties(part.path("then"), facts)
            } else result += properties(part, facts)
        }
        return result
    }

    /** Property names to offer: the object's properties, plus what would pick a branch it hasn't chosen yet. */
    private fun keySuggestions(node: JsonNode, facts: Pair<Set<String>, String?>): List<Pair<String, JsonNode>> {
        val schema = resolve(node)
        val choosers = if (schema.has("if") && !facts.first.containsAll(schema.path("if").path("required").map { it.asText() })) {
            chooserKeys(schema)
        } else emptyList()
        return (choosers + properties(node, facts).toList()).distinctBy { it.first }
    }

    /** The keys that select a branch of an if/then/else, e.g. "type" or "ref" for a view, with their schemas. */
    private fun chooserKeys(schema: JsonNode): List<Pair<String, JsonNode>> {
        val result = ArrayList<Pair<String, JsonNode>>()
        val then = resolve(schema.path("then"))
        schema.path("if").path("required").forEach { key ->
            val name = key.asText()
            result += name to (then.path("properties").path(name).takeUnless { it.isMissingNode } ?: then)
        }
        val otherwise = resolve(schema.path("else"))
        if (otherwise.has("if")) result += chooserKeys(otherwise)
        return result
    }

    private fun items(node: JsonNode): JsonNode? = resolve(node).path("items").takeUnless { it.isMissingNode }

    private fun child(node: JsonNode, facts: Pair<Set<String>, String?>, slot: Any?): JsonNode? = when (slot) {
        is String -> properties(node, facts)[slot]
        is Int -> items(node)
        else -> null
    }

    /** Allowed values: enums and consts (including those in branches), and true/false for booleans. */
    private fun values(node: JsonNode): List<String> {
        val schema = resolve(node)
        val result = ArrayList<String>()
        schema.path("enum").forEach { if (!it.isNull) result += it.asText() }
        schema.path("const").takeIf { it.isValueNode }?.let { result += it.asText() }
        if (types(schema).contains("boolean")) result += listOf("true", "false")
        return result.distinct()
    }

    private fun types(schema: JsonNode): Set<String> {
        val type = schema.path("type")
        return if (type.isArray) type.map { it.asText() }.toSet() else setOfNotNull(type.asText(null))
    }

    /** What to put after "name": for a property, so the cursor lands where the value goes. */
    private fun template(node: JsonNode): String {
        val schema = resolve(node)
        val types = types(schema)
        return when {
            schema.has("enum") && schema.path("enum").any { it.isTextual } -> "\"\""
            schema.has("const") && schema.path("const").isTextual -> "\"${schema.path("const").asText()}\""
            "string" in types -> "\"\""
            "array" in types -> "[]"
            "object" in types || schema.has("if") || schema.has("properties") || schema.has("allOf") -> "{}"
            "boolean" in types -> "false"
            else -> ""
        }
    }

    private fun literal(value: String, node: JsonNode): String {
        val schema = resolve(node)
        val textual = schema.path("enum").any { it.isTextual && it.asText() == value } ||
            (schema.path("const").isTextual && schema.path("const").asText() == value)
        return if (textual) "\"$value\"" else value
    }
}
