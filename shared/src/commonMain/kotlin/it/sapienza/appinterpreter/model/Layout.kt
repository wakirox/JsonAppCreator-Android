package it.sapienza.appinterpreter.model

import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.view_model.helper.MView
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
class Layout(
    override val id: String? = null,
    override val action: Action? = null,
    override val mapping: String? = null,
    override val data: JsonObject? = null,
    val orientation: LayoutOrientation? = LayoutOrientation.vertical,
    val views: List<MView> = emptyList(),
    override val visibleIf: String? = null
) : MView()
