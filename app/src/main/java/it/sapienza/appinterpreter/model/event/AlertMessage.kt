package it.sapienza.appinterpreter.model.event

import com.fasterxml.jackson.annotation.JsonProperty

class AlertMessage(
    var value : String?,
    @param:JsonProperty("thenDoOK") thenDoOKObject : EventObject?,
    @param:JsonProperty("thenDoKO") thenDoKOObject : EventObject?,
    mapping : String?,
    data : MutableMap<Any?, Any?>?) : Event(mapping,data) {

    val thenDoOK = thenDoOKObject?.eventInstance
    val thenDoKO = thenDoKOObject?.eventInstance
}
