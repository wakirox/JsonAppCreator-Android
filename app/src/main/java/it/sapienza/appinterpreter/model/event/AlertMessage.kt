package it.sapienza.appinterpreter.model.event

import com.fasterxml.jackson.annotation.JsonProperty

class AlertMessage(
    var value : String?,
    @JsonProperty("thenDoOK") var _thenDoOK : EventObject?,
    @JsonProperty("thenDoKO") var _thenDoKO : EventObject?,
    mapping : String?,
    data : MutableMap<Any?, Any?>?) : Event(mapping,data) {


    val thenDoOK = _thenDoOK?.eventInstance
    val thenDoKO = _thenDoKO?.eventInstance
}