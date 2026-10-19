package it.sapienza.appinterpreter.model

import it.sapienza.appinterpreter.Fixtures
import it.sapienza.appinterpreter.json.JsonText
import it.sapienza.appinterpreter.model.event.AlertMessage
import it.sapienza.appinterpreter.model.event.CalendarEvent
import it.sapienza.appinterpreter.model.event.RESTService
import it.sapienza.appinterpreter.model.event.ShowView
import it.sapienza.appinterpreter.model.view_model.ButtonView
import it.sapienza.appinterpreter.model.view_model.FormView
import it.sapienza.appinterpreter.model.view_model.ListView
import it.sapienza.appinterpreter.model.view_model.TextView
import it.sapienza.appinterpreter.model.view_model.form_model.CheckBox
import it.sapienza.appinterpreter.model.view_model.form_model.DateInput
import it.sapienza.appinterpreter.model.view_model.form_model.JacksonDateSerializer
import it.sapienza.appinterpreter.model.view_model.form_model.Selector
import it.sapienza.appinterpreter.model.view_model.form_model.TextInput
import it.sapienza.appinterpreter.model.view_model.form_model.helpers.TextType
import it.sapienza.appinterpreter.model.view_model.helper.TextStyle
import it.sapienza.appinterpreter.model.view_model.helper.ViewReference
import it.sapienza.appinterpreter.utils.AppParser
import it.sapienza.appinterpreter.utils.ConfigurationValidationException
import it.sapienza.appinterpreter.utils.FormDates
import it.sapienza.appinterpreter.validation.ConfigurationValidator
import it.sapienza.appinterpreter.validation.V3Schema
import it.sapienza.appinterpreter.validation.ValidatedConfigurationParser
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModelSerializationTest {
    private val parser = ValidatedConfigurationParser(ConfigurationValidator.fromSchema(V3Schema.JSON))
    private val withDefaults = Json(ModelJson) { encodeDefaults = true; explicitNulls = true }

    @Test
    fun everyBundledConfigurationParses() {
        Fixtures.configurations.forEach { (name, json) ->
            val app = parser.parse(json)
            assertTrue(app.name.isNotBlank(), name)
        }
    }

    @Test
    fun everyBundledConfigurationSurvivesARoundTrip() {
        Fixtures.configurations.forEach { (name, json) ->
            val app = AppParser(json).parseApplication2()
            val written = ModelJson.encodeToString(ModelApplication.serializer(), app)
            val again = ModelJson.encodeToString(ModelApplication.serializer(), ModelJson.decodeFromString(ModelApplication.serializer(), written))
            assertEquals(written, again, name)
            // Everything the file says is in the model: the model with its defaults written out contains the file.
            assertContains(JsonText.parse(json)!!, withDefaults.encodeToJsonElement(ModelApplication.serializer(), app), name, "$")
        }
    }

    @Test
    fun typesChooseTheClassAndObjectsWithoutATypeAreReferences() {
        val app = AppParser(
            """
            {
              "name": "Types", "version": "1",
              "mainView": { "type": "layout", "orientation": null, "views": [
                { "type": "text", "label": "Hi", "textSize": 18 },
                { "type": "button", "title": "Go", "action": { "ref": "go" } },
                { "id": "card" },
                { "ref": "card" },
                { "type": "list", "itemView": { "type": "text", "mapping": "name" },
                  "initData": { "url": "https://example.com/x", "thenDo": { "type": "alert", "value": "Done" } } },
                { "type": "form", "formElements": [
                  { "type": "text", "mapping": "name", "title": "Name", "textType": "email" },
                  { "type": "selector", "mapping": "size", "title": "Size", "options": ["S", "M"], "selection": "L" },
                  { "type": "date", "mapping": "day", "title": "Day", "value": "2026-05-01" },
                  { "type": "checkbox", "mapping": "ok", "title": "OK", "mandatory": true }
                ] }
              ] },
              "views": [ { "id": "card", "type": "text", "size": { "width": 20.0 } } ],
              "actions": [ { "id": "go", "type": "longclick", "event": { "type": "show", "view": { "ref": "card" } } },
                           { "id": "cal", "event": { "type": "calendar", "title": "Race", "date": "01/05/2026", "durationMinutes": 90 } } ]
            }
            """
        ).parseApplication2()
        val views = (app.mainView as Layout).views
        assertNull((app.mainView as Layout).orientation)
        assertEquals(18f, assertIs<TextView>(views[0]).textSize)
        assertEquals(TextStyle.regular, (views[0] as TextView).textStyle)
        assertTrue(assertIs<ButtonView>(views[1]).action!!.isReference())
        assertEquals("card", assertIs<ViewReference>(views[2]).referenceId)
        assertEquals("card", assertIs<ViewReference>(views[3]).referenceId)
        val list = assertIs<ListView>(views[4])
        assertIs<AlertMessage>(assertIs<RESTService>(list.initData).thenDo)
        val fields = assertIs<FormView>(views[5]).formElements
        assertEquals(TextType.email, assertIs<TextInput>(fields[0]).textType)
        assertNull(assertIs<Selector>(fields[1]).defaultValue())
        // UTC midnight, shown as that instant's day where the phone is.
        assertEquals(FormDates.format(JacksonDateSerializer.parse("2026-05-01")!!), assertIs<DateInput>(fields[2]).defaultValue())
        assertTrue(assertIs<CheckBox>(fields[3]).isRequired)
        assertEquals(20, (app.viewBy("card") as TextView).size?.width)
        assertIs<ShowView>(app.actions.first { it.id == "go" }.event)
        assertEquals(90, assertIs<CalendarEvent>(app.actions.first { it.id == "cal" }.event).durationMinutes)
    }

    @Test
    fun mistakesTheSchemaWouldCatchStillFailToParse() {
        listOf(
            """{ "type": "picture" }""",
            """{ "type": "text", "colour": "red" }""",
            """{ "type": "list" }""",
            """{ "type": "text", "action": { "id": "a", "event": { "type": "toast" } } }""",
            """{ "type": "form", "formElements": [ { "type": "selector", "mapping": "a", "title": "A" } ] }"""
        ).forEach { view ->
            val error = assertFailsWith<ConfigurationValidationException>(view) {
                AppParser("""{ "name": "Broken", "version": "1", "mainView": $view }""").parseApplication2()
            }
            assertEquals("$", error.jsonPath)
            assertTrue(error.detail.startsWith("configuration cannot be parsed"), error.detail)
        }
    }

    @Test
    fun datesAreReadAsJacksonReadThem() {
        val may = LocalDate(2026, 5, 1).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        assertEquals(may, JacksonDateSerializer.parse("2026-05-01"))
        assertEquals(may, JacksonDateSerializer.parse(may.toString()))
        assertEquals(may + 9 * 3_600_000 + 30 * 60_000, JacksonDateSerializer.parse("2026-05-01T09:30"))
        assertEquals(may + 7 * 3_600_000 + 30 * 60_000 + 250, JacksonDateSerializer.parse("2026-05-01T09:30:00.250+02:00"))
        assertEquals(LocalDate(2026, 3, 2).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(), JacksonDateSerializer.parse("2026-02-30"))
        assertNull(JacksonDateSerializer.parse("1 May 2026"))
    }

    @Test
    fun formDatesAreStrictAsSimpleDateFormatWas() {
        assertEquals(LocalDate(2026, 2, 1), FormDates.parseDate("1/2/2026"))
        assertEquals(LocalDate(2026, 3, 12), FormDates.parseDate("12/03/2026 and more"))
        assertNull(FormDates.parseDate("31/02/2026"))
        assertNull(FormDates.parseDate("2026-03-12"))
        assertNull(FormDates.parseDateTime("12/03/2026 24:00"))
        assertEquals("05/01/0026", FormDates.format(LocalDate(26, 1, 5)))
        assertEquals("01/05/2026", FormDates.format(LocalDate(2026, 5, 1).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(), TimeZone.UTC))
    }

    /** Whether everything in [expected] is in [actual], numbers compared by value (20 is 20.0). */
    private fun assertContains(expected: JsonElement, actual: JsonElement?, name: String, path: String) {
        when (expected) {
            is JsonObject -> {
                val actualObject = assertIs<JsonObject>(actual, "$name $path")
                expected.forEach { (key, value) -> assertContains(value, actualObject[key], name, "$path.$key") }
            }
            is JsonArray -> {
                val actualArray = assertIs<JsonArray>(actual, "$name $path")
                assertEquals(expected.size, actualArray.size, "$name $path")
                expected.forEachIndexed { index, value -> assertContains(value, actualArray[index], name, "$path[$index]") }
            }
            is JsonNull -> assertTrue(actual == null || actual is JsonNull, "$name $path")
            is JsonPrimitive -> {
                val actualPrimitive = assertIs<JsonPrimitive>(actual, "$name $path")
                if (expected.isString) assertEquals(expected, actualPrimitive, "$name $path")
                else assertEquals(expected.content.toDoubleOrNull() ?: expected.content, actualPrimitive.content.toDoubleOrNull() ?: actualPrimitive.content, "$name $path")
            }
        }
    }
}
