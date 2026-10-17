package it.sapienza.appinterpreter.validation

import android.content.Context
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.InputFormat
import com.networknt.schema.Schema
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SchemaRegistryConfig
import com.networknt.schema.SpecificationVersion
import com.networknt.schema.path.PathType
import java.io.InputStream

enum class ConfigurationIssueKind {
    MALFORMED_JSON,
    SCHEMA
}

data class ConfigurationIssue(
    val path: String,
    val message: String,
    val kind: ConfigurationIssueKind
)

data class ConfigurationValidationResult(
    val issues: List<ConfigurationIssue>
) {
    val isValid: Boolean
        get() = issues.isEmpty()
}

/**
 * Validates Appkin input before it is converted into the Kotlin model.
 *
 * The validator is immutable and may be reused. It deliberately reports schema
 * failures as values so callers can show all useful paths instead of surfacing
 * only the first Jackson conversion exception.
 */
class ConfigurationValidator private constructor(
    private val schema: Schema,
    private val mapper: ObjectMapper = ObjectMapper()
) {
    fun validate(json: String): ConfigurationValidationResult {
        val document = try {
            mapper.readTree(json)
        } catch (error: JsonProcessingException) {
            val location = error.location
            val position = if (location == null) {
                ""
            } else {
                " at line ${location.lineNr}, column ${location.columnNr}"
            }
            return ConfigurationValidationResult(
                listOf(
                    ConfigurationIssue(
                        path = "$",
                        message = "Invalid JSON$position: ${error.originalMessage}",
                        kind = ConfigurationIssueKind.MALFORMED_JSON
                    )
                )
            )
        }

        if (document == null || document.isMissingNode) {
            return ConfigurationValidationResult(
                listOf(
                    ConfigurationIssue(
                        path = "$",
                        message = "Configuration is empty",
                        kind = ConfigurationIssueKind.MALFORMED_JSON
                    )
                )
            )
        }

        val issues = schema.validate(json, InputFormat.JSON)
            .map { validationMessage ->
                val path = validationMessage.instanceLocation.toString()
                    .takeIf(String::isNotBlank) ?: "$"
                ConfigurationIssue(
                    path = path,
                    message = validationMessage.message
                        .removePrefix("$path: ")
                        .ifBlank { "Value does not match the v3 configuration schema" },
                    kind = ConfigurationIssueKind.SCHEMA
                )
            }
            .distinctBy { Triple(it.path, it.message, it.kind) }
            .sortedWith(compareBy(ConfigurationIssue::path, ConfigurationIssue::message))

        return ConfigurationValidationResult(issues)
    }

    companion object {
        const val ASSET_PATH = "v3.schema.json"

        fun fromAssets(context: Context): ConfigurationValidator =
            context.assets.open(ASSET_PATH).use { fromSchema(it) }

        fun fromSchema(schemaInput: InputStream): ConfigurationValidator {
            val schema = SchemaRegistry
                .withDefaultDialect(SpecificationVersion.DRAFT_2020_12) { builder ->
                    builder.schemaRegistryConfig(
                        SchemaRegistryConfig.builder()
                            .pathType(PathType.LEGACY)
                            .build()
                    )
                }
                .getSchema(schemaInput)
            return ConfigurationValidator(schema)
        }
    }
}
