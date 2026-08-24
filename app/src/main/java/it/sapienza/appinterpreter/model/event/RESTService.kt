package it.sapienza.appinterpreter.model.event

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Serve per riempire le liste con dati paginati
 */
abstract class DataService (
    @param:JsonProperty("thenDo") thenDoObject : EventObject?,
    mapping : String?,
    data : MutableMap<Any?, Any?>?
) : Event(mapping, data){
    val thenDo = thenDoObject?.eventInstance
}

class RESTService (val url : String,
                   val method : CallServiceMethod? = CallServiceMethod.get,
                   @param:JsonProperty("thenDo") thenDoObject : EventObject?,
                   mapping : String?,
                   data : MutableMap<Any?, Any?>?) : DataService(thenDoObject, mapping, data)
