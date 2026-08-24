package it.sapienza.appinterpreter.validation

import it.sapienza.appinterpreter.model.ModelApplication
import it.sapienza.appinterpreter.utils.AppParser

/** Runs structural schema validation and semantic reference validation as one operation. */
class ValidatedConfigurationParser(
    private val validator: ConfigurationValidator
) {
    fun parse(json: String): ModelApplication {
        val result = validator.validate(json)
        if (!result.isValid) throw ConfigurationSchemaException(result.issues)
        return AppParser(json).parseApplication2()
    }
}

class ConfigurationSchemaException(
    val issues: List<ConfigurationIssue>
) : IllegalArgumentException(
    buildString {
        append("Configuration validation failed")
        issues.forEach { issue ->
            append("\n• ")
            append(issue.path)
            append(": ")
            append(issue.message)
        }
    }
)
