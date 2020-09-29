package it.sapienza.appinterpreter.model.view_model.helper

import it.sapienza.appinterpreter.model.action.Action

open class View(
    var id : String?,
    var action : Action?,
    var mapping : String?,
    var data : MutableMap<Any?, Any?>?
)