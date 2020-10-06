package it.sapienza.appinterpreter.model.event

import it.sapienza.appinterpreter.model.view_model.helper.MView

class ShowView(
    var view : MView,
    mapping : String?,
    data : MutableMap<Any?, Any?>?
) : Event(mapping, data)