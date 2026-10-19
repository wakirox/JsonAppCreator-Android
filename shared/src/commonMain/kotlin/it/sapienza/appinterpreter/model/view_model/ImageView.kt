package it.sapienza.appinterpreter.model.view_model

import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.view_model.helper.MView
import it.sapienza.appinterpreter.model.view_model.helper.Size
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
class ImageView(
    val url: String? = null,
    val size: Size? = null,
    override val id: String? = null,
    override val action: Action? = null,
    override val mapping: String? = null,
    override val data: JsonObject? = null,
    override val visibleIf: String? = null
) : MView()
