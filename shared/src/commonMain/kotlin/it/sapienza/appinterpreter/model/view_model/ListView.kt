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
    override val data: JsonObject? = null
) : MView()
