package it.sapienza.appinterpreter.model.view_model

import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.view_model.helper.Size
import it.sapienza.appinterpreter.model.view_model.helper.TextStyle

class ButtonView(
    textSize : Float? = null,
    size : Size? = null,
    textStyle: TextStyle? = TextStyle.regular,
    title : String?,
    label : String? = null,
    id : String?,
    action : Action?,
    mapping : String?,
    data : MutableMap<Any?, Any?>?
) : TextView(textSize, size, textStyle, title, label, id, action, mapping, data)