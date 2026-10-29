package it.sapienza.appinterpreter.validation

import io.github.optimumcode.json.schema.JsonSchema
import io.github.optimumcode.json.schema.ValidationError
import it.sapienza.appinterpreter.json.JsonNumbers
import it.sapienza.appinterpreter.json.JsonSyntaxException
import it.sapienza.appinterpreter.json.JsonText
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

enum class ConfigurationIssueKind {
    MALFORMED_JSON,
    SCHEMA
}

data class ConfigurationIssue(val path: String, val message: String, val kind: ConfigurationIssueKind)

data class ConfigurationValidationResult(val issues: List<ConfigurationIssue>) {
    val isValid: Boolean
        get() = issues.isEmpty()
}

/**
 * Validates Appkin input before it is converted into the Kotlin model.
 *
 * The validator is immutable and may be reused. It deliberately reports schema
 * failures as values so callers can show all useful paths instead of surfacing
 * only the first conversion exception.
 */
class ConfigurationValidator private constructor(private val schema: JsonSchema, private val schemaJson: JsonElement) {

    fun validate(json: String): ConfigurationValidationResult {
        val document = try {
            JsonText.parse(json)
        } catch (error: JsonSyntaxException) {
            return malformed("Invalid JSON at line ${error.line}, column ${error.column}: ${error.reason}")
        } ?: return malformed("Configuration is empty")
        return validate(document)
    }

    /** The schema issues of an already parsed [document]. */
    fun validate(document: JsonElement): ConfigurationValidationResult {
        val errors = ArrayList<ValidationError>()
        schema.validate(document, errors::add)
        val issues = errors
            .flatMap { error -> issues(error, document) }
            .distinctBy { Triple(it.path, it.message, it.kind) }
            .sortedWith(compareBy(ConfigurationIssue::path, ConfigurationIssue::message))
        return ConfigurationValidationResult(issues)
    }

    /**
     * An error as the issues the app has always shown, worded as the networknt validator did (the editor and the AI
     * fix request show these messages, and the editor reads the unknown property's name from its message).
     */
    private fun issues(error: ValidationError, document: JsonElement): List<ConfigurationIssue> {
        val pointer = error.objectPath.toString()
        val path = jsonPath(pointer, document)
        val location = error.absoluteLocation?.path?.toString() ?: error.schemaPath.toString()
        val keyword = location.substringAfterLast('/')
        val rule = elementAt(schemaJson, location)
        val instance = elementAt(document, pointer)
        fun issue(message: String, at: String = path) = ConfigurationIssue(at, message, ConfigurationIssueKind.SCHEMA)
        val messages = when (keyword) {
            // An unknown property is reported on its object, so the editor points at the property by its name.
            "additionalProperties" -> {
                val cut = pointer.lastIndexOf('/')
                return listOf(
                    issue(
                        "property '${unescape(
                            pointer.substring(cut + 1)
                        )}' is not defined in the schema and the schema does not allow additional properties",
                        at = jsonPath(pointer.substring(0, cut), document)
                    )
                )
            }

            // One issue for each missing property.
            "required" -> (rule as? JsonArray).orEmpty()
                .map { (it as JsonPrimitive).content }
                .filter { name -> (instance as? JsonObject)?.containsKey(name) != true }
                .map { issue("required property '$it' not found") }

            "enum" -> listOf(
                issue(
                    "does not have a value in the enumeration [" +
                        (rule as? JsonArray).orEmpty().joinToString(", ") { value ->
                            when {
                                value is JsonNull -> ""
                                value is JsonPrimitive && value.isString -> "\"${value.content}\""
                                else -> value.toString()
                            }
                        } + "]"
                )
            )

            "const" -> listOf(issue("must be the constant value '${(rule as? JsonPrimitive)?.content ?: rule}'"))

            "type" -> listOf(issue("${typeOf(instance)} found, ${expectedTypes(rule)} expected"))

            "minimum" -> listOf(issue("must have a minimum value of ${(rule as? JsonPrimitive)?.content}"))

            "minLength" -> listOf(issue("must be at least ${(rule as? JsonPrimitive)?.content} characters long"))

            else -> listOf(issue(error.message.ifBlank { "Value does not match the v3 configuration schema" }))
        }
        // A "oneOf" whose null branch failed failed as a whole: networknt said so too, at the same place.
        return if (ONE_OF_BRANCH_TYPE.containsMatchIn(location)) {
            messages + issue("must be valid to one and only one schema, but 0 are valid")
        } else {
            messages
        }
    }

    private fun malformed(message: String) = ConfigurationValidationResult(
        listOf(ConfigurationIssue(path = "$", message = message, kind = ConfigurationIssueKind.MALFORMED_JSON))
    )

    companion object {
        const val ASSET_PATH = "v3.schema.json"

        fun fromSchema(schemaJson: String): ConfigurationValidator =
            ConfigurationValidator(JsonSchema.fromDefinition(schemaJson), checkNotNull(JsonText.parse(schemaJson)))

        private val ONE_OF_BRANCH_TYPE = Regex("/oneOf/\\d+/type$")

        /** The element at a JSON Pointer, or null. */
        private fun elementAt(root: JsonElement, pointer: String): JsonElement? {
            if (pointer.isEmpty() || pointer == "/") return root
            var current: JsonElement? = root
            pointer.removePrefix("/").split('/').map(::unescape).forEach { segment ->
                current = when (val node = current) {
                    is JsonObject -> node[segment]
                    is JsonArray -> segment.toIntOrNull()?.let(node::getOrNull)
                    else -> null
                }
            }
            return current
        }

        /** The JSON type of a value as networknt named it: an integer is a whole number, a number has a fraction. */
        private fun typeOf(value: JsonElement?): String = when (value) {
            null, is JsonNull -> "null"

            is JsonObject -> "object"

            is JsonArray -> "array"

            is JsonPrimitive -> when {
                value.isString -> "string"
                value.content == "true" || value.content == "false" -> "boolean"
                JsonNumbers.isDecimal(value.content) -> "number"
                else -> "integer"
            }
        }

        private fun expectedTypes(rule: JsonElement?): String = when (rule) {
            is JsonArray -> rule.joinToString(", ", "[", "]") { (it as JsonPrimitive).content }
            is JsonPrimitive -> rule.content
            else -> "valid"
        }

        /**
         * A JSON Pointer ("/mainView/views/1/type") as the JSONPath the app shows ("$.mainView.views[1].type"): an
         * index where the document has an array, a property name elsewhere.
         */
        fun jsonPath(pointer: String, document: JsonElement): String {
            if (pointer.isEmpty() || pointer == "/") return "$"
            val path = StringBuilder("$")
            var current: JsonElement? = document
            pointer.removePrefix("/").split('/').map(::unescape).forEach { segment ->
                val index = segment.toIntOrNull()
                current = when (val node = current) {
                    is JsonArray -> if (index != null) {
                        path.append('[').append(index).append(']')
                        node.getOrNull(index)
                    } else {
                        path.append('.').append(segment)
                        null
                    }

                    is JsonObject -> {
                        path.append('.').append(segment)
                        node[segment]
                    }

                    else -> {
                        path.append('.').append(segment)
                        null
                    }
                }
            }
            return path.toString()
        }

        private fun unescape(segment: String) = segment.replace("~1", "/").replace("~0", "~")
    }
}
