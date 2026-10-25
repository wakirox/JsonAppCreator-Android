package it.sapienza.appinterpreter.ai

import it.sapienza.appinterpreter.editor.EditorIssue
import it.sapienza.appinterpreter.json.JsonText
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The parts of the reference the editor's problems are in, so a fix request carries what the model needs to fix them:
 * for each problem's path, the innermost view, form field, event, action or nested part (stored, sort, initData,
 * size) it reaches in the configuration.
 */
internal class IssueSections(private val reference: SchemaReference) {

    /** Each part once, in the order the problems name them, as its title and its reference text. */
    fun of(json: String, issues: List<EditorIssue>): List<Pair<String, String>> {
        val tree = runCatching { JsonText.parse(json) }.getOrNull() ?: return emptyList()
        return issues.mapNotNull { sectionAt(tree, it.path) }.distinct().map { section ->
            section.title to buildString { with(reference) { write(section) } }
        }
    }

    private fun sectionAt(tree: JsonElement, path: String): SchemaReference.Section? {
        val segments = SEGMENT.findAll(path.removePrefix("$")).map { it.groupValues[1].ifEmpty { it.groupValues[2] } }.toList()
        var node: JsonElement? = tree
        var key: String? = null
        var found: SchemaReference.Section? = null
        for (segment in segments) {
            node = when (val current = node) {
                is JsonObject -> current[segment].also { key = segment }
                is JsonArray -> segment.toIntOrNull()?.let { current.getOrNull(it) }
                else -> null
            } ?: break
            (node as? JsonObject)?.let { objectNode -> partOf(key, objectNode)?.let { found = it } }
        }
        return found
    }

    /** The part an object reached through [key] is, if any: a view, form field or event by its type, or a nested part. */
    private fun partOf(key: String?, node: JsonObject): SchemaReference.Section? {
        val type = (node["type"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        return when (key) {
            in VIEWS -> type?.let { reference.section(SchemaReference.Kind.View, it) }
            "formElements" -> type?.let { reference.section(SchemaReference.Kind.FormField, it) }
            in EVENTS -> type?.let { reference.section(SchemaReference.Kind.Event, it) }
            in ACTIONS -> if ("ref" in node) null else reference.section(SchemaReference.Kind.Nested, "actionDefinition")
            else -> NESTED[key]?.let { reference.section(SchemaReference.Kind.Nested, it) }
        }
    }

    private companion object {
        /** ".name" or "[0]" (a path from the validator, such as $.mainView.views[2].stored.sort[0]). */
        val SEGMENT = Regex("""\.([^.\[]+)|\[(\d+)]""")
        val VIEWS = setOf("mainView", "views", "itemView", "view")
        val EVENTS = setOf("event", "thenDo", "thenDoOK", "thenDoKO", "onError")
        val ACTIONS = setOf("action", "actions", "swipeToStart", "swipeToEnd")
        val NESTED = mapOf("initData" to "initialRestService", "stored" to "storedData", "sort" to "storedSort", "size" to "size")
    }
}
