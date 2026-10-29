package it.sapienza.appinterpreter.ai

import it.sapienza.appinterpreter.editor.EditorIssue
import it.sapienza.appinterpreter.validation.V3Schema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReferencePartsTest {
    private val reference = SchemaReference(V3Schema.JSON)

    @Test
    fun anchorsAreGitHubsWithRepeatsNumberedAndCodeSkipped() {
        val anchors = GitHubAnchors.of(
            """
            ## Example 1: a checklist, with a detail screen
            #### `text`
            ```json
            # not a heading
            ```
            ### Form fields
            #### `text`
            #### Order of a stored list (`sort`)
            """.trimIndent()
        )
        assertEquals(
            "example-1-a-checklist-with-a-detail-screen",
            anchors.of("Example 1: a checklist, with a detail screen")
        )
        assertEquals("text", anchors.of("`text`"))
        assertEquals("text-1", anchors.of("`text`", after = "Form fields"))
        assertEquals("order-of-a-stored-list-sort", anchors.of("Order of a stored list (`sort`)"))
        assertNull(anchors.of("not a heading"))
    }

    @Test
    fun aProblemBringsTheReferenceOfTheInnermostPartItIsIn() {
        val json = """
            { "schemaVersion": 3, "name": "Trip", "version": "1.0",
              "mainView": { "type": "layout", "views": [
                { "type": "text", "title": "Hi" },
                { "type": "form", "formElements": [{ "type": "date", "mapping": "d" }] },
                { "type": "list", "itemView": { "type": "text" },
                  "stored": { "collection": "places", "sort": [{ "field": "visited" }] },
                  "action": { "id": "a", "event": { "type": "save", "collection": "x", "thenDo": { "type": "alert" } } } }
              ] } }
        """
        fun titles(vararg paths: String) =
            IssueSections(reference).of(json, paths.map { EditorIssue(null, it, "wrong") }).map { it.first }

        assertEquals(listOf("Order of a stored list (`sort`)"), titles("$.mainView.views[2].stored.sort[0]"))
        assertEquals(listOf("Saved data for a list (`stored`)"), titles("$.mainView.views[2].stored"))
        assertEquals(listOf("Form field `date`"), titles("$.mainView.views[1].formElements[0]"))
        assertEquals(listOf("Event `alert`"), titles("$.mainView.views[2].action.event.thenDo"))
        assertEquals(
            listOf("View `text`", "View `list`"),
            titles("$.mainView.views[0].title", "$.mainView.views[2]", "$.mainView.views[2].orientation")
        )
        assertEquals(emptyList<String>(), titles("$.name"))
        val sort = IssueSections(
            reference
        ).of(json, listOf(EditorIssue(null, "$.mainView.views[2].stored.sort[0]", "x"))).single().second
        assertTrue(sort.contains("- `by` **(required)**"), sort)
    }

    @Test
    fun aFixRequestCarriesTheReferenceItNeeds() {
        val json = """{ "schemaVersion": 3, "name": "T", "version": "1", "mainView": { "type": "list", """ +
            """"itemView": { "type": "text" }, "stored": { "collection": "c", "sort": { "field": "x" } } } }"""
        val request = AiContext.fixRequest(
            json,
            listOf(EditorIssue(null, "$.mainView.stored.sort", "property 'field' is not defined")),
            V3Schema.JSON
        )
        assertTrue(request.contains("The reference for these parts:\n\n#### Order of a stored list (`sort`)"), request)
        assertTrue(request.indexOf("#### Order of a stored list") < request.indexOf("```json"))
        assertTrue(
            !AiContext.fixRequest(json, emptyList()).contains("The reference"),
            "without the schema, just the problems"
        )
    }
}
