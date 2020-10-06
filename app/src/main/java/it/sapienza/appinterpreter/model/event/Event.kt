package it.sapienza.appinterpreter.model.event

open class Event(
    var mapping : String?,
    var data : MutableMap<Any?, Any?>?
)