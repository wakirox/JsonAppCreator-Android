package it.sapienza.appinterpreter.model.view_model

import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.view_model.helper.Size
import it.sapienza.appinterpreter.model.view_model.helper.MView

class ImageView(
    var url : String?,
    var originalUrl : String?,
    var size : Size?,
    id : String?,
    action : Action?,
    mapping : String?,
    data : MutableMap<Any?, Any?>?
) : MView(id, action, mapping, data) {

    override fun isEmpty(): Boolean {
        return super.isEmpty() && url == null
    }
}