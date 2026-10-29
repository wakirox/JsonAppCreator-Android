package it.sapienza.appinterpreter.runtime

import it.sapienza.appinterpreter.json.getValue
import it.sapienza.appinterpreter.json.hasValueAt
import it.sapienza.appinterpreter.json.matchReplace
import it.sapienza.appinterpreter.model.view_model.TextFormat
import it.sapienza.appinterpreter.model.view_model.TextView
import it.sapienza.appinterpreter.model.view_model.TextualView
import it.sapienza.appinterpreter.model.view_model.helper.MView
import kotlinx.serialization.json.JsonObject

/** Which views are drawn: a view whose data lacks what it shows takes no space, label included. */
object ViewVisibility {
    private val TAG = Regex("<[^>]*>")

    /**
     * True when [view] is drawn with [data] (the view's own data, else its parent's). [reference] is the reference
     * [view] was reached through, if any: its visibleIf counts too.
     */
    fun isShown(view: MView, data: JsonObject?, reference: MView? = null): Boolean =
        holds(reference?.takeIf { it !== view }?.visibleIf, data) && holds(view.visibleIf, data) &&
            hasContent(view, data)

    /** A visibleIf condition: "path" needs a value there, "!path" needs none, and null always holds. */
    fun holds(condition: String?, data: JsonObject?): Boolean {
        val trimmed = condition?.trim()
        if (trimmed.isNullOrEmpty()) return true
        return if (trimmed.startsWith("!")) !data.hasValueAt(trimmed.drop(1).trim()) else data.hasValueAt(trimmed)
    }

    /**
     * A text view has nothing to show when its mapping (or its title, after placeholders) is empty, unless it says
     * showWhenEmpty; a view with only a label is a heading and stays. Other views always have content.
     */
    private fun hasContent(view: MView, data: JsonObject?): Boolean {
        if (view !is TextView) return true
        // HTML with nothing but tags shows nothing.
        val text = view.text(data).let { if (view.format == TextFormat.html) it.replace(TAG, "") else it }
        val label = view.label?.matchReplace(data).orEmpty()
        if (text.isBlank() && label.isBlank()) return false
        val hasSource = view.mapping != null || view.title != null
        return view.showWhenEmpty == true || !hasSource || text.isNotBlank()
    }
}

/** The text a text view or button shows: the value at its mapping, else its title, with placeholders filled in. */
fun TextualView.text(data: JsonObject?): String =
    ((mapping?.let { data?.getValue(it) } ?: title).orEmpty()).matchReplace(data)
