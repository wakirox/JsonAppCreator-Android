package it.sapienza.appinterpreter.model.view_model

import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.event.RESTService
import it.sapienza.appinterpreter.model.view_model.helper.MView
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
class ListView(
    val itemView: MView,
    val initData: RESTService? = null,
    val orientation: LayoutOrientation = LayoutOrientation.vertical,
    override val id: String? = null,
    override val action: Action? = null,
    override val mapping: String? = null,
    override val data: JsonObject? = null,
    override val visibleIf: String? = null,
    /** Items per row of a grid; 2 by default. */
    val columns: Int? = null,
    /** What an empty list says instead of "No data available"; may use placeholders. */
    val emptyText: String? = null
) : MView() {
    /** Items per row: 1 unless the list is a grid. */
    val columnCount: Int get() = if (orientation == LayoutOrientation.grid) (columns ?: 2).coerceIn(1, 6) else 1
}
