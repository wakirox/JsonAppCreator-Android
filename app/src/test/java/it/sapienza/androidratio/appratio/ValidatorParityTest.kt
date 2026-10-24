package it.sapienza.androidratio.appratio

import it.sapienza.appinterpreter.validation.ConfigurationIssue
import it.sapienza.appinterpreter.validation.ConfigurationIssueKind
import it.sapienza.appinterpreter.validation.ConfigurationValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

/**
 * The multiplatform validator (OptimumCode, kotlinx.serialization) against the one the app used before (networknt on
 * Jackson): every bundled configuration and a set of deliberately broken ones must give the same error paths, such as
 * `$.mainView.views[1].type`. The messages are worded differently; build/reports/validator-parity.txt lists both.
 */
class ValidatorParityTest {
    private val schema = Files.readString(Path.of("..", "schema", "v3.schema.json"))
    private val reference by lazy { NetworkntValidator.fromSchema(schema.byteInputStream()) }
    private val validator by lazy { ConfigurationValidator.fromSchema(schema) }

    private val configurations: List<Pair<String, String>> by lazy {
        listOf(Path.of("src", "main", "res", "raw"), Path.of("..", "samples"), Path.of("..", "examples"))
            .flatMap { directory -> Files.list(directory).use { it.toList() } }
            .filter { it.name.endsWith(".json") }
            .sorted()
            .map { "${it.parent.name}/${it.name}" to Files.readString(it) }
    }

    @Test
    fun everyBundledConfigurationIsValidForBoth() {
        assertTrue(configurations.size >= 10)
        configurations.forEach { (name, json) ->
            assertEquals("$name (networknt)", emptyList<ConfigurationIssue>(), reference.validate(json).issues)
            assertEquals("$name (multiplatform)", emptyList<ConfigurationIssue>(), validator.validate(json).issues)
        }
    }

    @Test
    fun brokenConfigurationsGiveTheSamePaths() {
        val report = StringBuilder()
        val differences = mutableListOf<String>()
        broken().forEach { (name, json) ->
            val old = reference.validate(json).issues
            val new = validator.validate(json).issues
            report.appendLine("## $name")
            old.forEach { report.appendLine("  networknt      ${it.path}: ${it.message}") }
            new.forEach { report.appendLine("  multiplatform  ${it.path}: ${it.message}") }
            if (old.isEmpty()) differences += "$name is valid for networknt: the case doesn't test anything"
            if (old != new) {
                differences += "$name\n  networknt:     $old\n  multiplatform: $new"
            }
        }
        val output = Path.of("build", "reports", "validator-parity.txt")
        Files.createDirectories(output.parent)
        Files.writeString(output, report.toString() + "\n${broken().size} cases\n")
        assertEquals("Paths differ:\n" + differences.joinToString("\n"), emptyList<String>(), differences)
    }

    @Test
    fun malformedJsonIsReportedAtTheRootByBoth() {
        listOf("{ \"name\": ", "{ \"name\": \"a\", }", "", "[1 2]").forEach { json ->
            assertEquals(json, ConfigurationIssueKind.MALFORMED_JSON, reference.validate(json).issues.single().kind)
            assertEquals(json, ConfigurationIssueKind.MALFORMED_JSON, validator.validate(json).issues.single().kind)
        }
    }

    /** Broken variants of the bundled configurations: one mistake each, of every kind the schema checks. */
    private fun broken(): List<Pair<String, String>> {
        val hello = configuration("v3_hello.json")
        val masterpieces = configuration("v3_masterpieces.json")
        val race = configuration("v3_race_signup.json")
        val tmdb = configuration("v3_the_movie_db.json")
        val catalog = configuration("v3_static_catalog.json")
        val openAccessArt = configuration("v3_open_access_art.json")
        val all = configurations.map { it.second }
        val cases = mutableListOf<Pair<String, String>>()
        fun case(name: String, source: String, from: String, to: String) {
            check(from in source) { "$name: '$from' not found" }
            cases += name to source.replaceFirst(from, to)
        }
        // Every configuration: schema version, root fields, root shape.
        all.forEachIndexed { index, json ->
            case("schemaVersion 4 in #$index", json, "\"schemaVersion\": 3", "\"schemaVersion\": 4")
            case("unknown root property in #$index", json, "\"schemaVersion\": 3", "\"schemaVersion\": 3, \"theme\": \"dark\"")
            case("root name missing in #$index", json, "\"name\":", "\"nome\":")
            case("mainView a string in #$index", json, "\"mainView\": {", "\"mainView\": \"home\", \"x\": {")
        }
        case("view type unknown", hello, "\"type\": \"text\"", "\"type\": \"txet\"")
        case("view property unknown", hello, "\"type\": \"text\"", "\"type\": \"text\", \"colour\": \"red\"")
        case("textSize a string", hello, "\"textSize\": 20", "\"textSize\": \"big\"")
        case("textStyle unknown", hello, "\"textStyle\": \"bold\"", "\"textStyle\": \"underline\"")
        case("name empty", hello, "\"name\": \"", "\"name\": \"\", \"was\": \"")
        case("layout without views", hello, "\"views\": [", "\"children\": [")
        case("layout orientation unknown", hello, "\"type\": \"layout\"", "\"type\": \"layout\", \"orientation\": \"diagonal\"")
        case("mainView null", hello, "\"mainView\": {", "\"mainView\": null, \"x\": {")
        case("root an array", hello, hello, "[$hello]")
        case("legacy reference with an extra property", masterpieces, "\"mainView\": {", "\"mainView\": { \"id\": \"x\", \"extra\": 1 }, \"y\": {")
        case("reference mixed with a definition", hello, "\"type\": \"layout\"", "\"ref\": \"home\", \"type\": \"layout\"")
        case("empty reference", hello, "\"mainView\": {", "\"mainView\": { \"ref\": \"\" }, \"y\": {")
        case("list without itemView", masterpieces, "\"itemView\"", "\"itemVue\"")
        case("list orientation unknown", masterpieces, "\"type\": \"list\"", "\"type\": \"list\", \"orientation\": \"circle\"")
        case("text size negative", hello, "\"textSize\": 20", "\"textSize\": 20, \"size\": { \"width\": -1 }")
        case("text size with depth", hello, "\"textSize\": 20", "\"textSize\": 20, \"size\": { \"depth\": 3, \"height\": 1.5 }")
        case("image url a number", hello, "\"views\": [", "\"views\": [ { \"type\": \"image\", \"url\": 42 },")
        case("data not an object", masterpieces, "\"type\": \"list\"", "\"type\": \"list\", \"data\": 5")
        case("registry view without type", hello, "\"mainView\": {", "\"views\": [ { \"id\": \"loose\" } ], \"mainView\": {")
        case("registry action without event", race, "\"actions\": [", "\"actions\": [ { \"id\": \"nothing\" },")
        case("action type unknown", race, "\"event\": {", "\"type\": \"swipe\", \"event\": {")
        case("event type unknown", race, "\"type\": \"alert\"", "\"type\": \"toast\"")
        case("alert property unknown", race, "\"type\": \"alert\"", "\"type\": \"alert\", \"colour\": \"red\"")
        case("alert thenDoOK invalid", race, "\"type\": \"alert\"", "\"type\": \"alert\", \"thenDoOK\": { \"type\": \"show\" }")
        case("form element type unknown", race, "\"type\": \"checkbox\"", "\"type\": \"toggle\"")
        case("form element without mapping", race, "\"mapping\": \"name\"", "\"maping\": \"name\"")
        case("selector options not an array", race, "\"options\": [", "\"options\": \"S, M\", \"x\": [")
        case("mandatory a string", race, "\"mandatory\": true", "\"mandatory\": \"yes\"")
        case("form without formElements", race, "\"formElements\"", "\"fields\"")
        case("date min an object", race, "\"type\": \"date\"", "\"type\": \"date\", \"min\": { \"day\": 1 }")
        case("rest method patch", tmdb, "\"method\": \"get\"", "\"method\": \"patch\"")
        case("rest header not text", tmdb, "\"method\": \"get\"", "\"method\": \"get\", \"headers\": { \"X-Count\": 3 }")
        case("rest without url", tmdb, "\"url\":", "\"address\":")
        case("rest thenDo a string", tmdb, "\"thenDo\": {", "\"thenDo\": \"next\", \"was\": {")
        case("show without view", masterpieces, "\"type\": \"show\"", "\"type\": \"show\", \"vue\": 1")
        case("calendar duration 0", race, "\"type\": \"alert\"", "\"type\": \"calendar\", \"title\": \"Race\", \"date\": \"01/05/2026\", \"durationMinutes\": 0, \"was\": \"alert\"")
        case("calendar without date", race, "\"type\": \"alert\"", "\"type\": \"calendar\", \"title\": \"Race\"")
        case("button action a number", catalog, "\"type\": \"layout\"", "\"type\": \"layout\", \"action\": 7")
        case("debugMode a string", catalog, "\"name\":", "\"debugMode\": \"on\", \"name\":")
        case("open access art list initData without url", openAccessArt, "\"initData\": {", "\"initData\": { \"link\": 1,")
        cases += "deep mistakes" to """
            {
              "schemaVersion": 3, "name": "One typo", "version": "1.0",
              "mainView": { "type": "layout", "views": [
                { "type": "text", "label": "Title" },
                { "type": "list", "mapping": "items", "itemView": { "type": "layout", "views": [
                  { "type": "picture", "url": "https://example.com/a.png" },
                  { "type": "text", "label": "${'$'}{name}", "action": { "event": { "type": "alert", "colour": "red" } } }
                ] } }
              ] }
            }
        """.trimIndent()
        return cases
    }

    private fun configuration(name: String) = configurations.first { it.first.endsWith("/$name") }.second
}
