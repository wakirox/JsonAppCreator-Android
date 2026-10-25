package it.sapienza.appinterpreter.ai

import it.sapienza.appinterpreter.json.JsonNode
import it.sapienza.appinterpreter.json.JsonText
import it.sapienza.appinterpreter.json.sameValue
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import it.sapienza.appinterpreter.editor.ConfigurationChecker
import it.sapienza.appinterpreter.editor.EditorIssue
import it.sapienza.appinterpreter.editor.prettyJson

/**
 * Everything an AI assistant needs to write an Appkin configuration, in one Markdown text: the rules from
 * docs/LLM_GUIDE.md, a reference of every view, form field and event generated from the schema (so it can't drift
 * from it), complete examples, and optionally the app to change and what to build.
 */
object AiContext {

    data class Example(val title: String, val about: String, val json: String)

    fun build(
        guide: String,
        schemaJson: String,
        examples: List<Example>,
        currentApp: String? = null,
        request: String? = null
    ): String = buildString {
        appendLine("# Appkin: context for AI assistants")
        appendLine()
        appendLine(
            "Appkin is an Android app that turns a JSON configuration into a native app. You write the " +
                "configuration; the app validates it against the v3 schema and runs it. This document is the " +
                "complete contract: use only what it describes."
        )
        appendLine()
        appendLine(rules(guide).trim())
        appendLine()
        appendLine(SchemaReference(schemaJson).markdown().trim())
        examples.forEachIndexed { index, example ->
            appendLine()
            appendLine("## Example ${index + 1}: ${example.title}")
            appendLine()
            appendLine(example.about)
            appendLine()
            appendLine("```json")
            appendLine(example.json.trim())
            appendLine("```")
        }
        if (currentApp != null) {
            appendLine()
            appendLine("## The current configuration")
            appendLine()
            appendLine("Change this configuration; keep what the request doesn't mention.")
            appendLine()
            appendLine("```json")
            appendLine(currentApp.trim())
            appendLine("```")
        }
        appendLine()
        appendLine("## Your task")
        appendLine()
        val what = request?.trim()?.takeIf { it.isNotEmpty() } ?: "<describe the app: its screens, data and actions>"
        appendLine(
            if (currentApp != null) "Change the current configuration: $what"
            else "Create an Appkin v3 configuration for this app: $what"
        )
        appendLine()
        append(
            "If you can create files, give me the complete configuration as a downloadable file named " +
                "${fileName(currentApp)}, containing only the JSON. If you can't, reply with only the " +
                "complete JSON, starting with { and ending with }: no comments or explanations. I will open your " +
                "file or share your answer in the Appkin app, which runs it; if it reports problems, I " +
                "will send them to you to fix."
        )
        appendLine()
    }

    /**
     * The worked examples, from the bundled configurations: a checklist saved on the phone with a detail screen, a form
     * with an alert, and a list loaded from a REST API.
     */
    fun examples(checklist: String, signUpForm: String, restList: String): List<Example> = listOf(
        Example(
            "a checklist saved on the phone, with a detail screen",
            "The list starts with its `initialItems`, saved once; `unique` adds the ones a later version brings. Ticking " +
                "runs the `tick` update, and `sort` puts visited places last. Tapping an item shows the reusable `place` view.",
            prettyJson(checklist)
        ),
        Example(
            "a form with required fields and an alert",
            "Submitting runs the form's action with the filled-in values, so the alert can use `${'$'}{name}`.",
            prettyJson(signUpForm)
        ),
        Example(
            "a list loaded from a REST API, with a detail screen",
            "`initData` loads the JSON; `mapping` selects its array; each item's fields fill the item view and the detail.",
            prettyJson(restList)
        )
    )

    /**
     * A tree as the app wrote it with Jackson's default mapper and then formatted it: decimals as a Double prints
     * them, then pretty-printed.
     */
    private fun written(tree: JsonElement): String = prettyJson(JsonText.withDoubleNumbers(tree).toString())

    /** A file name for the configuration: from its "name", else "app.json". */
    fun fileName(json: String?): String {
        val name = json?.let { Regex(""""name"\s*:\s*"([^"]*)"""").find(it)?.groupValues?.get(1) }.orEmpty()
        val slug = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        return (slug.ifEmpty { "app" }) + ".json"
    }

    /** The guide's rules for a model: its sections without the repository-specific ones. */
    private fun rules(guide: String): String {
        val sections = guide.split(Regex("(?m)^(?=## )"))
        return sections.drop(1)
            .filterNot { section -> REPOSITORY_SECTIONS.any { section.startsWith("## $it") } }
            .joinToString("")
    }

    private val REPOSITORY_SECTIONS = listOf("Validate generated output", "Recommended prompt")

    /**
     * The JSON object in an AI answer: the first fenced block that holds one, else everything from the first { to the
     * last }. Null when there is none.
     */
    fun extractJson(answer: String): String? {
        val candidate = Regex("(?s)```[a-zA-Z]*\\s*\\n(.*?)```").findAll(answer)
            .map { it.groupValues[1].trim() }
            .firstOrNull { it.startsWith("{") }
            ?: answer.indexOf('{').let { start ->
                val end = answer.lastIndexOf('}')
                if (start >= 0 && end > start) answer.substring(start, end + 1).trim() else null
            }
            ?: return null
        val json = if (parses(candidate)) candidate else unescapeMarkdown(candidate).takeIf(::parses) ?: candidate
        return restorePlaceholders(json)
    }

    /**
     * The last configuration in a page or API response holding a whole conversation, such as a shared chat, where
     * the AI's answers sit inside JavaScript or JSON strings, escaped once or more: each level of escaping is undone
     * in turn until an object with "schemaVersion" and "mainView" parses. Exact copies of [promptExamples] (the
     * prompt's examples, which the conversation also contains) and the guide's skeleton are skipped; an AI's own app
     * is returned whatever its name, even one changed from an example.
     */
    fun lastConfiguration(page: String, promptExamples: Collection<String> = emptyList()): String? {
        val examples = promptExamples.mapNotNull { runCatching { JsonText.parse(it) }.getOrNull() }
        var text = page
        repeat(MAX_ESCAPING) {
            var at = text.lastIndexOf("\"schemaVersion\"")
            while (at >= 0) {
                var start = text.lastIndexOf('{', at)
                var tries = 0
                while (start >= 0 && tries < 64) {
                    val node = runCatching { JsonText.parseAt(text, start) }.getOrNull() as? JsonObject
                    if (node != null && "schemaVersion" in node && "mainView" in node) {
                        if (examples.none { JsonText.sameValue(it, node) } && JsonNode(node).path("name").asText() != GUIDE_SKELETON) {
                            return restorePlaceholders(written(node))
                        }
                        break
                    }
                    start = text.lastIndexOf('{', start - 1)
                    tries++
                }
                at = text.lastIndexOf("\"schemaVersion\"", at - 1)
            }
            text = unescapeString(text)
        }
        return null
    }

    private const val MAX_ESCAPING = 4

    /** The name in the guide's skeleton configuration, which every shared chat contains. */
    private const val GUIDE_SKELETON = "Application name"

    /** One level of JSON string escaping undone: \" becomes ", \n a line break, \u003c a <, \\ a \. */
    private fun unescapeString(text: String): String = buildString(text.length) {
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '\\' && i + 1 < text.length) {
                val next = text[i + 1]
                if (next == 'u' && i + 5 < text.length) {
                    text.substring(i + 2, i + 6).toIntOrNull(16)?.let { append(it.toChar()); i += 6; continue }
                }
                append(
                    when (next) {
                        'n' -> '\n'
                        't' -> '\t'
                        'r' -> '\r'
                        'b' -> '\b'
                        'f' -> '\u000C'
                        else -> next
                    }
                )
                i += 2
                continue
            }
            append(c)
            i++
        }
    }

    /**
     * AI answers sometimes write ${field} placeholders without the $ (Gemini wrote {thumb} and {title}), and chat apps
     * that render $...$ as maths can eat it too. JSON's own braces never enclose a bare name, so a {name} is always a
     * placeholder that lost its $, and gets it back.
     */
    private fun restorePlaceholders(json: String) = BARE_PLACEHOLDER.replace(json) { match ->
        // The space before the $ often went with it ("guests on{date}", "24{ingredient}"): put it back after words
        // and punctuation, but not inside URLs or names ("?c={category}", "item-{id}").
        val before = json.getOrNull(match.range.first - 1)
        val escape = json.getOrNull(match.range.first - 2) == '\\' // as in "\n{genre}": a line break, not a word
        val space = if (before != null && !escape && (before.isLetterOrDigit() || before in SPACE_AFTER)) " " else ""
        "$space\${${match.groupValues[1]}}"
    }

    private const val SPACE_AFTER = ":,;.!})•·"

    private val BARE_PLACEHOLDER = Regex("""(?<!\${'$'})\{([A-Za-z_][\w.]*)\}""")

    /**
     * Copying from a chat app can add Markdown escapes such as \[ and \_, which JSON never allows: they are removed.
     * Only used on text that isn't valid JSON as it is.
     */
    private fun unescapeMarkdown(text: String) = MARKDOWN_ESCAPE.replace(text, "$1")

    private val MARKDOWN_ESCAPE = Regex("""\\([\[\]{}()_*#`~<>!|+.-])""")

    private fun parses(json: String) = runCatching { JsonText.parse(json) }.isSuccess

    /** A message asking a model to fix the problems the editor found, with their lines and paths, and the JSON. */
    fun fixRequest(json: String, issues: List<EditorIssue>): String = buildString {
        appendLine("Appkin reports these problems in the configuration below:")
        appendLine()
        issues.forEach { issue ->
            val line = issue.offset?.let { ConfigurationChecker.lineOf(json, it) }
            val where = listOfNotNull(line?.let { "line $it" }, issue.path.takeIf { it != "$" }).joinToString(", ")
            appendLine("- ${if (where.isEmpty()) "" else "$where: "}${issue.message}")
        }
        appendLine()
        appendLine(
            "Fix them and give me the complete corrected configuration: as a downloadable file named " +
                "${fileName(json)} if you can create files, otherwise as only the JSON with no comments."
        )
        appendLine()
        appendLine("```json")
        appendLine(json.trim())
        append("```")
        appendLine()
    }
}

/** A readable reference of the v3 schema: the root, every view, form field and event, actions and references. */
private class SchemaReference(schemaJson: String) {
    private val root: JsonNode = JsonNode.parse(schemaJson)
    private val defs: JsonNode = root.path("\$defs")

    fun markdown(): String = buildString {
        appendLine("## Reference")
        appendLine()
        appendLine("Generated from the v3 JSON Schema. Unknown properties are rejected. **Required** properties are marked.")
        appendLine()
        appendLine("### Root object")
        appendLine()
        properties(root)
        section("Views", "viewDefinition", "Every view has a `type`. Where a view is expected, `{ \"ref\": \"id\" }` " +
            "may reference a view defined in the root `views` instead.")
        section("Form fields", "formElement", "The items of a form's `formElements`.")
        section("Events", "event", "What an action does: the `event` of an action definition. An event's `data`, like an " +
            "action's, is the data it runs with instead of the current data (a form's submitted values still come first).")
        appendLine("### Actions and references")
        appendLine()
        appendLine("Where an action is expected, write an action definition or `{ \"ref\": \"id\" }` referencing one in the root `actions`.")
        appendLine()
        listOf(
            "Action definition" to "actionDefinition",
            "REST data for a list (`initData`)" to "initialRestService",
            "Saved data for a list (`stored`)" to "storedData",
            "Order of a stored list (`sort`)" to "storedSort",
            "Size (`size`)" to "size"
        ).forEach { (title, def) ->
            appendLine("#### $title")
            appendLine()
            properties(defs.path(def))
        }
    }

    private fun StringBuilder.section(title: String, def: String, about: String) {
        appendLine("### $title")
        appendLine()
        appendLine(about)
        appendLine()
        defs.path(def).path("allOf").forEach { branch ->
            val type = branch.path("if").path("properties").path("type").path("const").asText()
            appendLine("#### `$type`")
            appendLine()
            properties(resolve(branch.path("then")), skip = setOf("type"))
        }
    }

    private fun StringBuilder.properties(node: JsonNode, skip: Set<String> = emptySet()) {
        val schema = resolve(node)
        schema.path("description").asText(null)?.let { appendLine(it); appendLine() }
        val required = schema.path("required").map { it.asText() }.toSet()
        schema.path("properties").fields().forEach { (name, value) ->
            if (name in skip) return@forEach
            val description = value.path("description").asText(null)
            append("- `$name`")
            if (name in required) append(" **(required)**")
            append(": ${describe(value)}")
            description?.let { append(". ${it.trimEnd('.')}") }
            appendLine()
        }
        appendLine()
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
}
