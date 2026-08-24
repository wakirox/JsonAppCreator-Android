package it.sapienza.appinterpreter.utils

import com.fasterxml.jackson.databind.JsonNode

/** Validates and indexes the reference graph before model conversion loses source paths. */
internal class ReferenceValidator {
    private data class LocatedReference(val id: String, val path: String)

    private val viewDefinitions = linkedMapOf<String, String>()
    private val actionDefinitions = linkedMapOf<String, String>()
    private val viewReferences = mutableListOf<LocatedReference>()
    private val actionReferences = mutableListOf<LocatedReference>()
    private val containmentEdges = linkedMapOf<String, MutableSet<String>>()

    fun validate(root: JsonNode) {
        if (!root.isObject) fail("$", "application configuration must be a JSON object")

        root.get("schemaVersion")?.let { version ->
            if (!version.isIntegralNumber || !version.canConvertToInt() || version.intValue() != 3) {
                fail("$.schemaVersion", "unsupported schema version; expected 3")
            }
        }

        root.get("views")?.let { views ->
            if (!views.isArray) fail("$.views", "must be an array")
            views.forEachIndexed { index, view ->
                visitView(view, "$.views[$index]", parentId = null, definitionRequired = true)
            }
        }

        root.get("actions")?.let { actions ->
            if (!actions.isArray) fail("$.actions", "must be an array")
            actions.forEachIndexed { index, action ->
                visitAction(action, "$.actions[$index]", definitionRequired = true)
            }
        }

        root.get("mainView")?.let { visitView(it, "$.mainView", parentId = null) }

        viewReferences.firstOrNull { it.id !in viewDefinitions }?.let {
            fail(it.path, "unresolved view reference '${it.id}'")
        }
        actionReferences.firstOrNull { it.id !in actionDefinitions }?.let {
            fail(it.path, "unresolved action reference '${it.id}'")
        }

        validateContainmentCycles()
    }

    private fun visitView(
        node: JsonNode,
        path: String,
        parentId: String?,
        definitionRequired: Boolean = false
    ) {
        if (!node.isObject) fail(path, "view must be an object")

        if (node.has("ref")) {
            if (definitionRequired) fail(path, "top-level views must be definitions, not references")
            val id = exclusiveReference(node, path, "view", "ref")
            addViewReference(id, "$path.ref", parentId)
            return
        }

        if (!node.has("type")) {
            if (definitionRequired) fail(path, "top-level views must include a type")
            if (node.size() != 1 || !node.has("id")) {
                fail(path, "view must be a definition with type or a reference with ref (legacy id-only references are also accepted)")
            }
            val id = requiredIdentifier(node.get("id"), "$path.id", "view reference")
            addViewReference(id, "$path.id", parentId)
            return
        }

        val id = node.get("id")?.let { requiredIdentifier(it, "$path.id", "view definition") }
        if (id != null) {
            registerDefinition(viewDefinitions, id, path, "view")
            parentId?.let { containmentEdges.getOrPut(it) { linkedSetOf() }.add(id) }
        }
        val ownerId = id ?: parentId

        node.get("views")?.let { children ->
            if (!children.isArray) fail("$path.views", "must be an array")
            children.forEachIndexed { index, child ->
                visitView(child, "$path.views[$index]", ownerId)
            }
        }
        node.get("itemView")?.let { visitView(it, "$path.itemView", ownerId) }
        node.get("action")?.let { visitAction(it, "$path.action") }
        node.get("initData")?.let { visitEvent(it, "$path.initData") }
    }

    private fun visitAction(node: JsonNode, path: String, definitionRequired: Boolean = false) {
        if (!node.isObject) fail(path, "action must be an object")

        if (node.has("ref")) {
            if (definitionRequired) fail(path, "top-level actions must be definitions, not references")
            val id = exclusiveReference(node, path, "action", "ref")
            actionReferences += LocatedReference(id, "$path.ref")
            return
        }

        val isDefinition = node.has("type") || node.has("event")
        if (!isDefinition) {
            if (definitionRequired) fail(path, "top-level actions must include an event")
            if (node.size() != 1 || !node.has("id")) {
                fail(path, "action must be a definition with an event or a reference with ref (legacy id-only references are also accepted)")
            }
            val id = requiredIdentifier(node.get("id"), "$path.id", "action reference")
            actionReferences += LocatedReference(id, "$path.id")
            return
        }

        val id = requiredIdentifier(node.get("id"), "$path.id", "action definition")
        registerDefinition(actionDefinitions, id, path, "action")
        val event = node.get("event") ?: fail("$path.event", "action definition must include an event")
        visitEvent(event, "$path.event")
    }

    private fun visitEvent(node: JsonNode, path: String) {
        if (!node.isObject) fail(path, "event must be an object")
        node.get("view")?.let { visitView(it, "$path.view", parentId = null) }
        node.get("thenDo")?.let { visitEvent(it, "$path.thenDo") }
        node.get("thenDoOK")?.let { visitEvent(it, "$path.thenDoOK") }
        node.get("thenDoKO")?.let { visitEvent(it, "$path.thenDoKO") }
    }

    private fun exclusiveReference(node: JsonNode, path: String, kind: String, field: String): String {
        if (node.size() != 1) {
            fail(path, "$kind reference must contain only '$field'; reference and definition fields cannot be mixed")
        }
        return requiredIdentifier(node.get(field), "$path.$field", "$kind reference")
    }

    private fun requiredIdentifier(node: JsonNode?, path: String, description: String): String {
        if (node == null || !node.isTextual || node.textValue().isBlank()) {
            fail(path, "$description id must be a non-empty string")
        }
        return node.textValue()
    }

    private fun registerDefinition(
        definitions: MutableMap<String, String>,
        id: String,
        path: String,
        kind: String
    ) {
        definitions.putIfAbsent(id, path)?.let { firstPath ->
            fail("$path.id", "duplicate $kind id '$id' (first defined at $firstPath)")
        }
    }

    private fun addViewReference(id: String, path: String, parentId: String?) {
        viewReferences += LocatedReference(id, path)
        parentId?.let { containmentEdges.getOrPut(it) { linkedSetOf() }.add(id) }
    }

    private fun validateContainmentCycles() {
        val visited = mutableSetOf<String>()
        val active = mutableListOf<String>()
        val activeSet = mutableSetOf<String>()

        fun visit(id: String) {
            if (id in activeSet) {
                val start = active.indexOf(id)
                val cycle = (active.subList(start, active.size) + id).joinToString(" -> ")
                fail(viewDefinitions[id] ?: "$", "view containment cycle detected: $cycle")
            }
            if (!visited.add(id)) return

            active += id
            activeSet += id
            containmentEdges[id].orEmpty().filter { it in viewDefinitions }.forEach(::visit)
            active.removeAt(active.lastIndex)
            activeSet -= id
        }

        viewDefinitions.keys.forEach(::visit)
    }

    private fun fail(path: String, detail: String): Nothing {
        throw ConfigurationValidationException(path, detail)
    }
}
