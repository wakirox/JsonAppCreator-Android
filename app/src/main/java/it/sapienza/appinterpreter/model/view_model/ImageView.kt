package it.sapienza.appinterpreter.model.view_model

import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.view_model.helper.Size
import it.sapienza.appinterpreter.model.view_model.helper.View
import it.sapienza.appinterpreter.model.view_model.helper.ViewElement

class ImageView(
    var url : String?,
    var size : Size?,
    mapping : String?
) : View(mapping), ViewElement {
    override var action: Action? = null
}