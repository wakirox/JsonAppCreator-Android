package it.sapienza.appinterpreter.model.event

class AlertMessage(
    var value : String?,
    var thenDoOK : EventObject?,
    var thenDoKO : EventObject?,
    mapping : String?,
    data : MutableMap<Any?, Any?>?) : Event(mapping,data)