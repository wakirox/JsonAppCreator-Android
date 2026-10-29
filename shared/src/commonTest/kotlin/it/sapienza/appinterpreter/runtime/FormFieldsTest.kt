package it.sapienza.appinterpreter.runtime

import androidx.lifecycle.SavedStateHandle
import it.sapienza.appinterpreter.calendar.CreateCalendarEventUseCase
import it.sapienza.appinterpreter.json.JsonText
import it.sapienza.appinterpreter.json.getValue
import it.sapienza.appinterpreter.model.event.CalendarEvent
import it.sapienza.appinterpreter.model.view_model.FormView
import it.sapienza.appinterpreter.model.view_model.form_model.Selector
import it.sapienza.appinterpreter.model.view_model.form_model.TimeInput
import it.sapienza.appinterpreter.utils.AppParser
import it.sapienza.appinterpreter.utils.FormDates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.JsonObject

/** Selector options with labels, time fields and hidden fields. */
class FormFieldsTest {
    private val form = AppParser(
        """
        {
          "name": "Fields", "version": "1.0", "schemaVersion": 3,
          "mainView": {
            "type": "form",
            "formElements": [
              { "type": "selector", "mapping": "type", "title": "Type", "mandatory": true, "selection": "",
                "options": [ { "label": "Any type", "value": "" }, "Painting", { "value": "Print" } ] },
              { "type": "time", "mapping": "time", "title": "Time", "value": "11:00" },
              { "type": "time", "mapping": "late", "title": "Late", "mandatory": true },
              { "type": "hidden", "mapping": "artwork.id", "value": "${'$'}{id}" },
              { "type": "hidden", "mapping": "title" },
              { "type": "hidden", "mapping": "note", "value": "${'$'}{missing}" }
            ]
          }
        }
        """
    ).parseApplication2().mainView as FormView
    private val data = JsonText.parse(
        """{ "id": 111654, "title": "South Wind, Clear Sky", "late": "9:5" }"""
    ) as JsonObject

    @Test
    fun optionsAreStringsOrLabelsWithValues() {
        val selector = assertIs<Selector>(form.formElements[0])
        assertEquals(listOf("Any type", "Painting", "Print"), selector.options.map { it.label })
        assertEquals(listOf("", "Painting", "Print"), selector.values)
        assertTrue(selector.isSet(""), "an empty value is an option like the others")
        assertFalse(selector.isSet("Any type"), "a label isn't a value")
        assertFalse(selector.isSet(null))
    }

    @Test
    fun timesAreHoursAndMinutes() {
        assertEquals(9 to 5, FormDates.parseTime("09:05"))
        assertEquals(9 to 5, FormDates.parseTime("9:05"))
        assertEquals(23 to 59, FormDates.parseTime(" 23:59 "))
        listOf("24:00", "12:60", "9:5", "12", "noon", "").forEach { assertNull(FormDates.parseTime(it), it) }
        assertEquals("09:05", FormDates.formatTime(9, 5))
        val late = assertIs<TimeInput>(form.formElements[2])
        assertTrue(late.isSet("07:30"))
        assertFalse(late.isSet("7.30"))
    }

    @Test
    fun startingValues() {
        val values = FormsState(SavedStateHandle()).values(form, data)
        assertEquals("", values["type"])
        assertEquals("11:00", values["time"])
        assertNull(values["late"], "9:5 isn't a time")
        assertEquals("111654", values["artwork.id"])
        assertEquals("South Wind, Clear Sky", values["title"], "without a value, the data's own")
        assertNull(values["note"])
    }

    @Test
    fun aFormThatAddsANewObjectKeepsOnlyItsHiddenFieldsFromTheData() {
        val values = FormsState(SavedStateHandle(), addsNew = { true }).values(form, data)
        assertEquals("11:00", values["time"], "defaults still apply")
        assertEquals("111654", values["artwork.id"])
        assertEquals("South Wind, Clear Sky", values["title"])
    }

    @Test
    fun hiddenFieldsAreSentWithTheForm() {
        val forms = FormsState(SavedStateHandle())
        assertNull(forms.submit(form, data))
        assertEquals("Please complete: Late", forms.state(form, data).error.value)

        forms.set(form, data, "late", "21:30")
        val json = assertNotNull(forms.submit(form, data))

        assertEquals("", json.getValue("type"))
        assertEquals("111654", json.getValue("artwork.id"))
        assertEquals("South Wind, Clear Sky", json.getValue("title"))
        assertEquals("21:30", json.getValue("late"))
        assertNull(json["note"])
    }

    @Test
    fun aTimeFieldTimesACalendarEvent() {
        val submitted = JsonText.parse("""{ "date": "20/10/2026", "time": "11:30" }""") as JsonObject
        val entry = assertIs<CreateCalendarEventUseCase.Result.Ready>(
            CreateCalendarEventUseCase {
                TimeZone.UTC
            }(CalendarEvent(title = "Visit", date = "${'$'}{date}", time = "${'$'}{time}"), submitted)
        ).entry
        assertFalse(entry.allDay)
        assertEquals(
            LocalDateTime(2026, 10, 20, 11, 30).toInstant(TimeZone.UTC).toEpochMilliseconds(),
            entry.beginMillis
        )
    }
}
