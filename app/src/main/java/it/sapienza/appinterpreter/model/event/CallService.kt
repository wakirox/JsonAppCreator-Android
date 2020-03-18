package it.sapienza.appinterpreter.model.event

class CallService(
    val url : String,
    //val mapping : String?,
    val method : CallServiceMethod? = CallServiceMethod.get,
    val thenDo : EventObject?
) : Event {




}