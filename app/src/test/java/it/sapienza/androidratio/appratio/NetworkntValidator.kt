package it.sapienza.androidratio.appratio

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.InputFormat
import com.networknt.schema.Schema
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SchemaRegistryConfig
import com.networknt.schema.SpecificationVersion
import com.networknt.schema.path.PathType
import it.sapienza.appinterpreter.validation.ConfigurationIssue
import it.sapienza.appinterpreter.validation.ConfigurationIssueKind
import it.sapienza.appinterpreter.validation.ConfigurationValidationResult
import java.io.InputStream

/**
 * The validator the app used until it moved to the multiplatform one (networknt on Jackson), kept for the parity
 * test only: it is the reference the new validator's paths are compared with.
 *
 * The validator is immutable and may be reused. It deliberately reports schema
 * failures as values so callers can show all useful paths instead of surfacing
 * only the first Jackson conversion exception.
 */
class NetworkntValidator private constructor(
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
        fun fromSchema(schemaInput: InputStream): NetworkntValidator {
            val schema = SchemaRegistry
                .withDefaultDialect(SpecificationVersion.DRAFT_2020_12) { builder ->
                    builder.schemaRegistryConfig(
                        SchemaRegistryConfig.builder()
                            .pathType(PathType.LEGACY)
                            .build()
                    )
                }
                .getSchema(schemaInput)
            return NetworkntValidator(schema)
        }
    }
}
