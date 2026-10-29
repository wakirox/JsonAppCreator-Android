package it.sapienza.androidratio.appratio

import com.networknt.schema.Error
import com.networknt.schema.InputFormat
import com.networknt.schema.Schema
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigSchemaValidationTest {
    private val schema: Schema by lazy {
        val schemaPath = Path.of("..", "schema", "v3.schema.json")
        Files.newInputStream(schemaPath).use {
            SchemaRegistry
                .withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
                .getSchema(it)
        }
    }

    @Test
    fun bundledV3ConfigurationsConformToSchema() {
        val configPaths = v3JsonFiles(Path.of("src", "main", "res", "raw")) +
            v3JsonFiles(Path.of("..", "samples")) +
            v3JsonFiles(Path.of("..", "examples"))

        assertTrue("No bundled v3 configurations were found", configPaths.isNotEmpty())
        configPaths.forEach(::assertValid)
    }

    @Test
    fun requestedConfigurationConformsToSchema() {
        val requestedPath = System.getProperty(REQUESTED_CONFIG_PROPERTY) ?: return
        assertValid(Path.of(requestedPath))
    }

    @Test
    fun acceptsExplicitAndLegacyViewReferences() {
        assertValid(
            """
            {
              "schemaVersion": 3,
              "name": "Reference compatibility",
              "version": "1.0",
              "mainView": { "ref": "home" },
              "views": [
                {
                  "id": "home",
                  "type": "layout",
                  "views": [
                    { "id": "legacy_text" },
                    { "type": "button", "title": "Run", "action": { "ref": "run" } }
                  ]
                },
                { "id": "legacy_text", "type": "text", "title": "Still supported" }
              ],
              "actions": [
                { "id": "run", "event": { "type": "alert", "value": "Done" } }
              ]
            }
            """.trimIndent()
        )
    }

    @Test
    fun rejectsUnsupportedSchemaVersion() {
        assertInvalid(
            """
            {
              "schemaVersion": 4,
              "name": "Future app",
              "version": "1.0",
              "mainView": { "type": "layout", "views": [] }
            }
            """.trimIndent()
        )
    }

    @Test
    fun rejectsReferenceCombinedWithDefinitionFields() {
        assertInvalid(
            """
            {
              "schemaVersion": 3,
              "name": "Ambiguous reference",
              "version": "1.0",
              "mainView": { "ref": "home", "type": "layout", "views": [] },
              "views": [{ "id": "home", "type": "layout", "views": [] }]
            }
            """.trimIndent()
        )
    }

    private fun v3JsonFiles(directory: Path): List<Path> {
        if (!Files.isDirectory(directory)) return emptyList()
        return Files.list(directory).use { paths ->
            paths
                .filter { it.isRegularFile() && it.name.startsWith("v3") && it.name.endsWith(".json") }
                .sorted()
                .toList()
        }
    }

    private fun assertValid(path: Path) {
        val errors = schema.validate(Files.readString(path), InputFormat.JSON)
        assertTrue(validationFailure(path.toString(), errors), errors.isEmpty())
    }

    private fun assertValid(json: String) {
        val errors = schema.validate(json, InputFormat.JSON)
        assertTrue(validationFailure("inline test configuration", errors), errors.isEmpty())
    }

    private fun assertInvalid(json: String) {
        assertTrue(
            "Expected configuration to be rejected",
            schema.validate(json, InputFormat.JSON).isNotEmpty()
        )
    }

    private fun validationFailure(path: String, errors: List<Error>): String = buildString {
        append("Configuration does not match schema/v3.schema.json: ")
        append(path)
        errors.sortedBy { it.message }.forEach { error ->
            append("\n - ")
            append(error.message)
        }
    }

    private companion object {
        const val REQUESTED_CONFIG_PROPERTY = "jsonApp.config"
    }
}
