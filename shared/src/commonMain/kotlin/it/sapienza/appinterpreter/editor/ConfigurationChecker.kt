package it.sapienza.appinterpreter.editor

import it.sapienza.appinterpreter.json.JsonSyntaxException
import it.sapienza.appinterpreter.json.JsonText
import it.sapienza.appinterpreter.utils.AppParser
import it.sapienza.appinterpreter.utils.ConfigurationValidationException
import it.sapienza.appinterpreter.validation.ConfigurationValidator

/** A problem in the edited text: where it is (a character offset, when known) and what is wrong. */
data class EditorIssue(val offset: Int?, val path: String, val message: String)

/**
 * The editor's live check: the same validation the app runs when it loads a configuration (JSON syntax, the v3
 * schema, then view and action references), with every problem placed in the text so the editor can jump to it.
 */
class ConfigurationChecker(private val validator: ConfigurationValidator) {

    fun check(json: String): List<EditorIssue> {
        // An empty text goes on to the validator, which says the configuration is empty.
        val tree = try {
            JsonText.parse(json)
        } catch (error: JsonSyntaxException) {
            return listOf(EditorIssue(error.offset.coerceIn(0, json.length), "$", syntaxMessage(error.reason)))
        }

        val schemaIssues = if (tree == null) validator.validate(json).issues else validator.validate(tree).issues
        if (schemaIssues.isNotEmpty()) {
            val offsets = pathOffsets(json)
            return schemaIssues.map { issue ->
                // An unknown property is reported on its object: point at the property itself.
                val unknown = UNKNOWN_PROPERTY.find(issue.message)?.groupValues?.get(1)
                val offset = unknown?.let { offsets["${issue.path}.$it"] } ?: offsetOf(issue.path, offsets)
                EditorIssue(offset, issue.path, issue.message)
            }
        }

        return try {
            AppParser(json).parse(tree)
            emptyList()
        } catch (error: ConfigurationValidationException) {
            listOf(EditorIssue(offsetOf(error.jsonPath, pathOffsets(json)), error.jsonPath, error.detail))
        } catch (error: Exception) {
            listOf(EditorIssue(null, "$", error.message ?: "The configuration could not be read"))
        }
    }

    companion object {
        private val UNKNOWN_PROPERTY = Regex("property '([^']+)' is not defined")

        /**
         * Where each JSON path starts in the text, as "$.mainView.views[1].type": a property at its key, an array
         * element at its first character.
         */
        fun pathOffsets(json: String): Map<String, Int> = JsonText.pathOffsets(json)

        /** The 1-based line of [offset] in [text]. */
        fun lineOf(text: String, offset: Int): Int = text.take(offset).count { it == '\n' } + 1

        /** The JSON reader's messages (worded as Jackson's were), in plain words for the common mistakes. */
        fun syntaxMessage(original: String): String = when {
            "CTRL-CHAR, code 10" in original -> "A text value isn't closed: add a quote (\") before the end of the line"
            original.startsWith("Unexpected end-of-input") -> "The JSON ends too early: a } or ] is missing"
            "double-quote to start field name" in original ->
                "A property name is missing its quotes, or there is an extra comma before }"
            "comma to separate" in original -> "A comma is missing between two items"
            "was expecting a colon" in original -> "A colon (:) is missing after the property name"
            original.startsWith("Unrecognized token") -> "Unknown word: text values need double quotes"
            else -> original.substringBefore('\n')
        }

        /** The offset of [path], or of its closest parent that exists in the text. */
        fun offsetOf(path: String, offsets: Map<String, Int>): Int? {
            var current = path
            while (true) {
                offsets[current]?.let { return it }
                val cut = maxOf(current.lastIndexOf('.'), current.lastIndexOf('['))
                if (cut <= 0) return offsets["$"]
                current = current.substring(0, cut)
            }
        }
    }
}
