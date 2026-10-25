package it.sapienza.appinterpreter.ai

import it.sapienza.appinterpreter.json.JsonNode

/**
 * A readable reference of the v3 schema: the root, every view, form field and event, actions and the parts nested in
 * them (a list's initData and stored, a sort, a size). Each part is a [Section], so the reference can be written whole,
 * as an outline, or a few parts at a time (the ones an error is about).
 */
internal class SchemaReference(schemaJson: String) {
    private val root: JsonNode = JsonNode.parse(schemaJson)
    private val defs: JsonNode = root.path("\$defs")

    enum class Kind(val label: String?) { Root(null), View("View"), FormField("Form field"), Event("Event"), Nested(null) }

    class Property(val name: String, val required: Boolean, val line: String)

    /** A view, form field or event by its [name] (its type), or a nested part by its schema definition. */
    class Section(val kind: Kind, val name: String, val heading: String, val about: String?, val properties: List<Property>) {
        /** The heading on its own, outside the reference: "View `list`", "Order of a stored list (`sort`)". */
        val title: String get() = kind.label?.let { "$it $heading" } ?: heading
    }

    class Group(val heading: String, val about: String?, val sections: List<Section>)

    val groups: List<Group> = listOf(
        Group("Root object", null, listOf(section(Kind.Root, "root", "Root object", root))),
        typed("Views", Kind.View, "viewDefinition", "Every view has a `type`. Where a view is expected, `{ \"ref\": \"id\" }` " +
            "may reference a view defined in the root `views` instead."),
        typed("Form fields", Kind.FormField, "formElement", "The items of a form's `formElements`."),
        typed("Events", Kind.Event, "event", "What an action does: the `event` of an action definition. An event's `data`, " +
            "like an action's, is the data it runs with instead of the current data (a form's submitted values still come first)."),
        Group(
            "Actions and references",
            "Where an action is expected, write an action definition or `{ \"ref\": \"id\" }` referencing one in the root `actions`.",
            NESTED.map { (title, def) -> section(Kind.Nested, def, title, defs.path(def)) }
        )
    )

    /** The section of [kind] named [name]: a type for views, form fields and events, a definition for nested parts. */
    fun section(kind: Kind, name: String): Section? =
        groups.flatMap { it.sections }.firstOrNull { it.kind == kind && it.name == name }

    fun markdown(): String = buildString {
        appendLine("## Reference")
        appendLine()
        appendLine("Generated from the v3 JSON Schema. Unknown properties are rejected. **Required** properties are marked.")
        appendLine()
        groups.forEach { group ->
            appendLine("### ${group.heading}")
            appendLine()
            group.about?.let { appendLine(it); appendLine() }
            group.sections.forEach { section ->
                if (section.kind != Kind.Root) {
                    appendLine("#### ${section.heading}")
                    appendLine()
                }
                write(section)
            }
        }
    }

    /** [section]'s description and property lines, then a blank line. */
    fun StringBuilder.write(section: Section) {
        section.about?.let { appendLine(it); appendLine() }
        section.properties.forEach { appendLine(it.line) }
        appendLine()
    }

    private fun typed(heading: String, kind: Kind, def: String, about: String) = Group(
        heading,
        about,
        defs.path(def).path("allOf").map { branch ->
            val type = branch.path("if").path("properties").path("type").path("const").asText()
            section(kind, type, "`$type`", resolve(branch.path("then")), skip = setOf("type"))
        }
    )

    private fun section(kind: Kind, name: String, heading: String, node: JsonNode, skip: Set<String> = emptySet()): Section {
        val schema = resolve(node)
        val required = schema.path("required").map { it.asText() }.toSet()
        val properties = schema.path("properties").fields().filter { (name, _) -> name !in skip }.map { (name, value) ->
            val line = buildString {
                append("- `$name`")
                if (name in required) append(" **(required)**")
                append(": ${describe(value)}")
                value.path("description").asText(null)?.let { append(". ${it.trimEnd('.')}") }
            }
            Property(name, name in required, line)
        }
        return Section(kind, name, heading, schema.path("description").asText(null), properties)
    }

    /** A short type description: "text", "number", one of the allowed values, a view, an array of form fields... */
    private fun describe(node: JsonNode): String {
        when (node.path("\$ref").asText("").removePrefix("#/\$defs/")) {
            "view" -> return "a view (or `{ \"ref\": \"id\" }`)"
            "viewDefinition" -> return "a view"
            "action" -> return "an action (or `{ \"ref\": \"id\" }`)"
            "actionDefinition" -> return "an action definition"
            "event" -> return "an event"
            "formElement" -> return "a form field"
            "data" -> return "an object with any data"
            "size" -> return "a size, `{ \"width\": number, \"height\": number }` in dp"
            "nonEmptyString" -> return "text (not empty)"
            "nullableString" -> return "text"
            "dateValue" -> return "a date, `dd/MM/yyyy`"
            "storedSort" -> return "a sort, `{ \"by\": \"field\" }` (see Order of a stored list), or a list of sorts"
            "initialRestService" -> return "a REST request, `{ \"url\": ..., \"method\": \"get\" | \"post\" }`"
            "" -> Unit
            else -> return describe(resolve(node))
        }
        node.path("oneOf").takeIf { it.isArray }?.let { options ->
            options.firstOrNull { it.path("type").asText() != "null" }?.let { return describe(it) }
        }
        node.path("const").takeIf { !it.isMissing }?.let { return "always `$it`" }
        node.path("enum").takeIf { it.isArray }?.let { values ->
            return "one of " + values.filterNot { it.isNull }.joinToString(", ") { "`$it`" }
        }
        val types = node.path("type").let { t -> if (t.isArray) t.map { it.asText() } else listOfNotNull(t.asText(null)) }
            .filter { it != "null" }
        if ("array" in types) return "an array; each item is " + describe(node.path("items"))
        val minimum = node.path("minimum").takeIf { it.isNumber }?.let { " (at least $it)" }.orEmpty()
        return when (types.firstOrNull()) {
            "string" -> "text"
            "integer" -> "a whole number$minimum"
            "number" -> "a number$minimum"
            "boolean" -> "`true` or `false`"
            "object" -> "an object"
            else -> "a value"
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

    companion object {
        /** The parts nested in views and actions, each with its schema definition. */
        val NESTED = listOf(
            "Action definition" to "actionDefinition",
            "REST data for a list (`initData`)" to "initialRestService",
            "Saved data for a list (`stored`)" to "storedData",
            "Order of a stored list (`sort`)" to "storedSort",
            "Size (`size`)" to "size"
        )
    }
}
