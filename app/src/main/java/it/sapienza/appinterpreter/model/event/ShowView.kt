package it.sapienza.appinterpreter.model.event

import it.sapienza.appinterpreter.model.screen.Screen
import it.sapienza.appinterpreter.model.view_model.helper.View

class ShowView(
    var view : View,
    mapping : String?,
    data : MutableMap<Any?, Any?>?
) : Event(mapping, data)