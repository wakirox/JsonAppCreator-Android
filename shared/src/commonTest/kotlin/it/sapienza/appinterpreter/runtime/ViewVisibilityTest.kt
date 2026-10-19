package it.sapienza.appinterpreter.runtime

import it.sapienza.appinterpreter.json.JsonText
import it.sapienza.appinterpreter.model.Layout
import it.sapienza.appinterpreter.model.view_model.helper.MView
import it.sapienza.appinterpreter.utils.AppParser
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Which views are drawn when the data lacks what they show. */
class ViewVisibilityTest {
    private val data = JsonText.parse(
        """{ "title": "Dora Wheeler", "creators": [ { "name": "Chase" } ], "current_location": "", "on_view": false }"""
    ) as JsonObject

    private fun app(views: String) = AppParser(
        """{ "name": "Visibility", "version": "1.0", "schemaVersion": 3,
             "mainView": { "type": "layout", "views": [ $views ] },
             "views": [ { "id": "visit", "type": "button", "title": "Plan a visit" } ] }"""
    ).parseApplication2()

    private fun views(json: String): List<MView> = (app(json).mainView as Layout).views

    private fun shown(json: String): Boolean = ViewVisibility.isShown(views(json).single(), data)

    @Test
    fun visibleIfNeedsAValueAtItsPath() {
        assertTrue(shown("""{ "type": "button", "title": "Artist", "visibleIf": "creators" }"""))
        assertFalse(shown("""{ "type": "button", "title": "Plan a visit", "visibleIf": "current_location" }"""))
        assertFalse(shown("""{ "type": "image", "url": "https://x/a.jpg", "visibleIf": "on_view" }"""))
        assertFalse(shown("""{ "type": "layout", "views": [], "visibleIf": "creators.1" }"""))
        assertTrue(shown("""{ "type": "layout", "views": [] }"""))
    }

    @Test
    fun anExclamationMarkShowsTheViewOnlyWithoutAValue() {
        assertTrue(shown("""{ "type": "text", "title": "Not on view", "visibleIf": "!current_location" }"""))
        assertFalse(shown("""{ "type": "text", "title": "Has an artist", "visibleIf": "!creators" }"""))
    }

    @Test
    fun aTextWithNothingToShowIsNotDrawnLabelIncluded() {
        assertTrue(shown("""{ "type": "text", "label": "Title", "mapping": "title" }"""))
        assertFalse(shown("""{ "type": "text", "label": "Technique", "mapping": "technique" }"""))
        assertFalse(shown("""{ "type": "text", "label": "On view", "mapping": "current_location" }"""))
        assertFalse(shown("""{ "type": "text", "label": "Made", "title": "${'$'}{creation_date}" }"""))
        assertFalse(shown("""{ "type": "text", "label": "${'$'}{missing}" }"""))
        // A mapping without a value falls back to the title, as it always did.
        assertTrue(shown("""{ "type": "text", "mapping": "technique", "title": "Unknown technique" }"""))
    }

    @Test
    fun aLabelAloneIsAHeadingAndStays() {
        assertTrue(shown("""{ "type": "text", "label": "Plan a visit" }"""))
    }

    @Test
    fun showWhenEmptyKeepsTheLabel() {
        assertTrue(shown("""{ "type": "text", "label": "Technique", "mapping": "technique", "showWhenEmpty": true }"""))
    }

    @Test
    fun aReferencesVisibleIfCountsToo() {
        val app = app("""{ "ref": "visit", "visibleIf": "current_location" }, { "ref": "visit" }""")
        val (hidden, plain) = (app.mainView as Layout).views
        assertFalse(ViewVisibility.isShown(app.viewBy(hidden)!!, data, reference = hidden))
        assertTrue(ViewVisibility.isShown(app.viewBy(plain)!!, data, reference = plain))
        assertEquals("visit", app.viewBy(hidden)?.id)
    }

    @Test
    fun conditionsAreTrimmedAndEmptyOnesHold() {
        assertTrue(ViewVisibility.holds(null, data))
        assertTrue(ViewVisibility.holds("", data))
        assertTrue(ViewVisibility.holds(" creators ", data))
        assertTrue(ViewVisibility.holds("! current_location", data))
        assertFalse(ViewVisibility.holds("title", null))
    }
}
