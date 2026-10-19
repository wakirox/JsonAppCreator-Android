package it.sapienza.appinterpreter.editor

import it.sapienza.appinterpreter.json.JsonNode

/**
 * A suggestion for the cursor: replace [start] until [end] with [insert], then put the cursor [cursor] characters into
 * the inserted text. [required] marks a property the object can't do without.
 */
data class Suggestion(
    val label: String,
    val start: Int,
    val end: Int,
    val insert: String,
    val cursor: Int,
    val required: Boolean = false
)

/**
 * The suggestions for the cursor. [heading] names the object they belong to (its type, or the property holding it)
 * when they are the attributes it can still take; it is null for values and for completing a half-typed name.
 */
data class Suggestions(val heading: String?, val items: List<Suggestion>) {
    companion object {
        val NONE = Suggestions(null, emptyList())
    }
}

/**
 * Autocomplete from the v3 JSON Schema: the attributes an object may still have (required ones first) and the
 * allowed values of the attribute being written. Views, form fields and events are told apart by their "type", as
 * the schema does.
 *
 * Where a new attribute name goes, they complete it at the cursor. Anywhere else in an object, for example while
 * writing a value, they list what the object is missing and add the chosen attribute at its end.
 */
class SchemaSuggestions(schemaJson: String) {
    private val root: JsonNode = JsonNode.parse(schemaJson)
    private val defs: JsonNode = root.path("\$defs")

    /** What the editor needs at the cursor, worked out from one reading of the text. */
    data class Assist(val suggestions: Suggestions, val numberKeyboard: Boolean)

    fun assist(text: String, cursor: Int): Assist {
        val located = locate(text, cursor) ?: return Assist(Suggestions.NONE, false)
        return Assist(suggestions(text, located), isNumber(text, cursor, located))
    }

    fun at(text: String, cursor: Int, limit: Int = 16): Suggestions =
        locate(text, cursor)?.let { suggestions(text, it, limit) } ?: Suggestions.NONE

    fun isNumberAt(text: String, cursor: Int): Boolean = locate(text, cursor)?.let { isNumber(text, cursor, it) } ?: false

    /** The cursor, the facts of each container around it, and the schema of the innermost one. */
    private class Located(val at: JsonCursor, val facts: List<Pair<Set<String>, String?>>, val schema: JsonNode)

    private fun locate(text: String, cursor: Int): Located? {
        val at = JsonCursor.at(text, cursor)
        if (at.position == CursorPosition.NONE) return null
        // The object facts are read without the token being typed, so a half-written string doesn't confuse them.
        val facts = text.removeRange(at.replaceStart, at.replaceEnd).let { clean ->
            at.containers.map { if (it.isObject) JsonCursor.objectFacts(clean, it.start) else emptySet<String>() to null }
        }
        var schema: JsonNode? = root
        for (i in 1 until at.containers.size) {
            schema = schema?.let { child(it, facts[i - 1], at.containers[i].slot) }
        }
        return schema?.let { Located(at, facts, it) }
    }

    private fun suggestions(text: String, located: Located, limit: Int = 16): Suggestions {
        val at = located.at
        val schema = located.schema
        val container = at.containers.last()
        val objectFacts = located.facts.last()
        val heading = objectFacts.second ?: (container.slot as? String) ?: if (at.containers.size == 1) "app" else null

        if (at.position == CursorPosition.VALUE) {
            val valueSchema = if (container.isObject) at.key?.let { properties(schema, objectFacts)[it] } else items(schema)
            val values = valueSchema?.let { values(it) }.orEmpty()
                .filter { it.startsWith(at.prefix, ignoreCase = true) && it != at.prefix }
                .take(limit)
                .map { value -> suggestion(value, at.replaceStart, at.replaceEnd, literal(value, valueSchema!!)) }
            if (values.isNotEmpty() || !container.isObject) return Suggestions(null, values)
            // Nothing to complete here: offer what the object is still missing, added at its end.
            val close = JsonCursor.closeOf(text, container.start).takeIf { it > 0 } ?: return Suggestions.NONE
            return Suggestions(heading, missing(schema, objectFacts).take(limit).map { (name, value, required) ->
                val (position, insert, inside) = memberAtEnd(text, container.start, close, "\"$name\": ${template(value)}")
                Suggestion(name, position, position, insert, inside, required)
            })
        }

        val typed = missing(schema, objectFacts)
            .filter { (name, _, _) -> name.startsWith(at.prefix, ignoreCase = true) && name != at.prefix }
            .take(limit)
            .map { (name, value, required) ->
                suggestion(name, at.replaceStart, at.replaceEnd, "\"$name\": ${template(value)}", required)
            }
        return Suggestions(if (at.prefix.isEmpty()) heading else null, typed)
    }

    /**
     * Whether the number keyboard fits the cursor: writing a value the schema says is a number (and not text), or
     * digits already typed, on the same line as its property name. On the next line the letters come back, for the
     * "}" or the next property.
     */
    private fun isNumber(text: String, cursor: Int, located: Located): Boolean {
        val at = located.at
        if (at.inString || at.position != CursorPosition.VALUE) return false
        val key = at.key?.takeIf { at.containers.last().isObject } ?: return false
        val colon = text.lastIndexOf(':', cursor - 1).takeIf { it >= 0 } ?: return false
        val written = text.substring(colon + 1, cursor)
        if ('\n' in written || !NUMBER_SO_FAR.matches(written.trim())) return false
        if (written.isNotBlank()) return true
        val types = properties(located.schema, located.facts.last())[key]?.let { types(resolve(it)) }.orEmpty()
        return ("number" in types || "integer" in types) && "string" !in types
    }

    private val NUMBER_SO_FAR = Regex("""-?\d*(\.\d*)?""")

    /** The attributes the object doesn't have yet, required ones first. */
    private fun missing(schema: JsonNode, facts: Pair<Set<String>, String?>): List<Triple<String, JsonNode, Boolean>> {
        val required = required(schema, facts)
        return keySuggestions(schema, facts)
            .filter { (name, _) -> name !in facts.first }
            .map { (name, value) -> Triple(name, value, name in required) }
            .sortedByDescending { it.third }
    }

    private fun suggestion(label: String, start: Int, end: Int, insert: String, required: Boolean = false): Suggestion {
        // Inside an empty string, object or array value; otherwise after the insertion.
        val inside = if (insert.endsWith("\"\"") || insert.endsWith("{}") || insert.endsWith("[]")) insert.length - 1 else insert.length
        return Suggestion(label, start, end, insert, inside, required)
    }

    /**
     * Where and how to add [member] as the last attribute of the object between [open] and [close]: after its last
     * attribute with a comma, on a new line like the others (or on the same line for a one-line object).
     * Returns the insertion offset, the text to insert and where the cursor goes in it.
     */
    private fun memberAtEnd(text: String, open: Int, close: Int, member: String): Triple<Int, String, Int> {
        var last = close - 1
        while (last > open && text[last].isWhitespace()) last--
        val empty = last == open
        val multiline = text.substring(open, close).contains('\n')
        val memberCursor = if (member.endsWith("\"\"") || member.endsWith("{}") || member.endsWith("[]")) member.length - 1 else member.length
        if (!multiline) {
            val prefix = if (empty) " " else ", "
            return Triple(last + 1, prefix + member, prefix.length + memberCursor)
        }
        val indent = if (empty) Snippets.indentAt(text, open) + "  " else Snippets.indentAt(text, last)
        val prefix = (if (empty) "" else ",") + "\n" + indent
        return Triple(last + 1, prefix + member, prefix.length + memberCursor)
    }

    /** The attributes the object must have, given the keys it already has and its "type". */
    private fun required(node: JsonNode, facts: Pair<Set<String>, String?>): Set<String> {
        val schema = resolve(node)
        val result = LinkedHashSet<String>()
        if (schema.has("if")) {
            val required = schema.path("if").path("required").map { it.asText() }
            result += if (facts.first.containsAll(required)) required(schema.path("then"), facts) else required
        }
        schema.path("required").forEach { result += it.asText() }
        schema.path("allOf").forEach { part ->
            if (part.has("if")) {
                val type = part.path("if").path("properties").path("type").path("const").asText(null)
                if (type != null && type == facts.second) result += required(part.path("then"), facts)
            } else result += required(part, facts)
        }
        return result
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
            result += name to (then.path("properties").path(name).takeUnless { it.isMissing } ?: then)
        }
        val otherwise = resolve(schema.path("else"))
        if (otherwise.has("if")) result += chooserKeys(otherwise)
        return result
    }

    private fun items(node: JsonNode): JsonNode? = resolve(node).path("items").takeUnless { it.isMissing }

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
