package it.sapienza.appinterpreter.model.view_model

import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.view_model.helper.Size
import it.sapienza.appinterpreter.model.view_model.helper.TextStyle
import it.sapienza.appinterpreter.model.view_model.helper.MView

open class TextView (
    var textSize : Float? = null,
    var size : Size? = null,
    var textStyle: TextStyle? = TextStyle.regular,
    var title : String? = null,
    var label : String? = null,
    id : String? = null,
    action : Action? = null,
    mapping : String? = null,
    data : MutableMap<Any?, Any?>? = null
) : MView(id,action,mapping,data) {

    override fun isEmpty(): Boolean {
        return super.isEmpty() && title == null && label == null
    }
}
