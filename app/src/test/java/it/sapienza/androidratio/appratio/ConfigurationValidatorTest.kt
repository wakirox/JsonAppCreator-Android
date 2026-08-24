package it.sapienza.androidratio.appratio

import it.sapienza.appinterpreter.validation.ConfigurationIssueKind
import it.sapienza.appinterpreter.validation.ConfigurationSchemaException
import it.sapienza.appinterpreter.validation.ConfigurationValidator
import it.sapienza.appinterpreter.validation.ValidatedConfigurationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class ConfigurationValidatorTest {
    private val validator: ConfigurationValidator by lazy {
        Files.newInputStream(Path.of("..", "schema", "v3.schema.json"))
            .use { ConfigurationValidator.fromSchema(it) }
    }
    private val configurationParser by lazy { ValidatedConfigurationParser(validator) }

    @Test
    fun menuConfigurationsPassTheInAppValidator() {
        val menuConfigs = listOf(
            "v3_app_sample.json",
            "v3_app_form.json",
            "v3_the_movie_db.json",
            "v3_pokedex.json"
        )

        menuConfigs.forEach { name ->
            val path = Path.of("src", "main", "res", "raw", name)
            val json = Files.newBufferedReader(path).use { it.readText() }
            val result = validator.validate(json)
            assertTrue(
                "$name failed validation:\n${formatIssues(result.issues)}",
                result.isValid
            )
            configurationParser.parse(json)
        }
    }

    @Test
    fun requestedConfigurationPassesTheInAppValidator() {
        val requestedPath = System.getProperty(REQUESTED_CONFIG_PROPERTY) ?: return
        val json = Files.newBufferedReader(Path.of(requestedPath)).use { it.readText() }
        configurationParser.parse(json)
    }

    @Test
    fun validLegacyConfigurationHasNoIssues() {
        val result = validator.validate(
            """
            {
              "name": "Hello",
              "version": "1.0",
              "mainView": { "type": "layout", "views": [] }
            }
            """.trimIndent()
        )

        assertTrue(result.isValid)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun malformedJsonIsReportedSeparatelyWithLocation() {
        val result = validator.validate(
            """
            {
              "name": "Broken",
              "version": "1.0",
            }
            """.trimIndent()
        )

        assertFalse(result.isValid)
        assertEquals(1, result.issues.size)
        assertEquals(ConfigurationIssueKind.MALFORMED_JSON, result.issues.single().kind)
        assertEquals("$", result.issues.single().path)
        assertTrue(result.issues.single().message.contains("line"))
        assertTrue(result.issues.single().message.contains("column"))
    }

    @Test
    fun structuralFailuresIncludeJsonPathsAndMultipleIssues() {
        val result = validator.validate(
            """
            {
              "schemaVersion": 4,
              "name": "Broken fields",
              "version": "1.0",
              "mainView": {
                "type": "layout",
                "orientation": "diagonal",
                "views": []
              }
            }
            """.trimIndent()
        )

        assertFalse(result.isValid)
        assertTrue(result.issues.all { it.kind == ConfigurationIssueKind.SCHEMA })
        assertTrue(result.issues.any { it.path == "$.schemaVersion" })
        assertTrue(result.issues.any { it.path == "$.mainView.orientation" })
        assertTrue(result.issues.size >= 2)
    }

    @Test
    fun combinedParserReturnsAllStructuralIssuesForDisplay() {
        val error = assertThrows(ConfigurationSchemaException::class.java) {
            configurationParser.parse(
                """
                {
                  "schemaVersion": 4,
                  "name": "Broken fields",
                  "version": "1.0",
                  "mainView": {
                    "type": "layout",
                    "orientation": "diagonal",
                    "views": []
                  }
                }
                """.trimIndent()
            )
        }

        assertTrue(error.issues.any { it.path == "$.schemaVersion" })
        assertTrue(error.issues.any { it.path == "$.mainView.orientation" })
        assertTrue(error.message.orEmpty().contains("• $.schemaVersion:"))
    }

    private fun formatIssues(issues: List<it.sapienza.appinterpreter.validation.ConfigurationIssue>): String =
        issues.joinToString("\n") { "${it.path}: ${it.message}" }

    private companion object {
        const val REQUESTED_CONFIG_PROPERTY = "jsonApp.config"
    }
}
