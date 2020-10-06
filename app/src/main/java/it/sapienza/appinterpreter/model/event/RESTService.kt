package it.sapienza.appinterpreter.model.event

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Serve per riempire le liste con dati paginati
 */
abstract class DataService (
    @JsonProperty("thenDo") var _thenDo : EventObject?,
    mapping : String?,
    data : MutableMap<Any?, Any?>?
) : Event(mapping, data){
    val thenDo = _thenDo?.eventInstance
}

class RESTService (val url : String,
                   val method : CallServiceMethod? = CallServiceMethod.get,
                   @JsonProperty("thenDo") _thenDo : EventObject?,
                   mapping : String?,
                   data : MutableMap<Any?, Any?>?) : DataService(_thenDo, mapping, data)