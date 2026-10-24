package it.sapienza.appinterpreter.model.view_model

import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.view_model.helper.MView
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * A tick box with [title] (${field} placeholders allowed) that shows whether the data's true/false value at [mapping]
 * is true. Ticking it runs [action] with the data where that value is turned over, so an update event saves it at
 * once; a ticked item's title is struck through.
 */
@Serializable
class CheckView(
    val title: String? = null,
    override val mapping: String,
    override val id: String? = null,
    override val action: Action? = null,
    override val data: JsonObject? = null,
    override val visibleIf: String? = null
) : MView()
