package it.sapienza.appinterpreter.model.event

/**
 * Serve per riempire le liste con dati paginati
 */
abstract class DataService (
    var thenDo : EventObject?,
    mapping : String?,
    data : MutableMap<Any?, Any?>?
) : Event(mapping, data)

class RESTService (val url : String,
                   val method : CallServiceMethod? = CallServiceMethod.get,
                   thenDo : EventObject?,
                   mapping : String?,
                   data : MutableMap<Any?, Any?>?) : DataService(thenDo, mapping, data)