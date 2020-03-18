package it.sapienza.appinterpreter.model.event

class AlertMessage(
    var value : String?,
    var mapping : String?,
    var thenDoOK : EventObject?,
    var thenDoKO : EventObject?) : Event