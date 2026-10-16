package it.sapienza.appinterpreter.editor

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.ObjectMapper
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
        try {
            ObjectMapper().readTree(json) ?: return listOf(EditorIssue(0, "$", "The configuration is empty"))
        } catch (error: JsonProcessingException) {
            val offset = error.location?.charOffset?.toInt()?.coerceIn(0, json.length)
            return listOf(EditorIssue(offset, "$", syntaxMessage(error.originalMessage)))
        }

        val schemaIssues = validator.validate(json).issues
        if (schemaIssues.isNotEmpty()) {
            val offsets = pathOffsets(json)
            return schemaIssues.map { EditorIssue(offsetOf(it.path, offsets), it.path, it.message) }
        }

        return try {
            AppParser(json).parseApplication2()
            emptyList()
        } catch (error: ConfigurationValidationException) {
            listOf(EditorIssue(offsetOf(error.jsonPath, pathOffsets(json)), error.jsonPath, error.detail))
        } catch (error: Exception) {
            listOf(EditorIssue(null, "$", error.message ?: "The configuration could not be read"))
        }
    }

    companion object {
        /**
         * Where each JSON path starts in the text, as "$.mainView.views[1].type": a property at its key, an array
         * element at its first character.
         */
        fun pathOffsets(json: String): Map<String, Int> {
            val offsets = HashMap<String, Int>()
            try {
                JsonFactory().createParser(json).use { parser ->
                    while (true) {
                        val token = parser.nextToken() ?: break
                        val context = parser.parsingContext
                        // On "{" or "[" Jackson has already entered the new container, so an array element that is
                        // a container is found through its parent.
                        val elementOf = when (token) {
                            JsonToken.FIELD_NAME -> context
                            JsonToken.START_OBJECT, JsonToken.START_ARRAY -> context.parent?.takeIf { it.inArray() }
                            JsonToken.END_OBJECT, JsonToken.END_ARRAY -> null
                            else -> context.takeIf { it.inArray() }
                        } ?: continue
                        offsets.putIfAbsent(elementOf.toPath(), parser.currentTokenLocation().charOffset.toInt())
                    }
                }
            } catch (_: JsonProcessingException) {
                // A partial map still places the issues before the syntax error.
            }
            offsets.putIfAbsent("$", 0)
            return offsets
        }

        /** Jackson's parser messages, in plain words for the common mistakes. */
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

        private fun com.fasterxml.jackson.core.JsonStreamContext.toPath(): String {
            val segments = ArrayDeque<String>()
            var context: com.fasterxml.jackson.core.JsonStreamContext? = this
            while (context != null && !context.inRoot()) {
                when {
                    context.inObject() -> context.currentName?.let { segments.addFirst(".$it") }
                    context.inArray() -> segments.addFirst("[${maxOf(context.currentIndex, 0)}]")
                }
                context = context.parent
            }
            return "$" + segments.joinToString("")
        }
    }
}
