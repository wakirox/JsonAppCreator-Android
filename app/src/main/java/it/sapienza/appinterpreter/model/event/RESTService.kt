package it.sapienza.appinterpreter.model.event

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Serve per riempire le liste con dati paginati
 */
abstract class DataService(
    @JsonProperty("thenDo") var _thenDo: EventObject?,
    mapping: String?,
    data: MutableMap<Any?, Any?>?
) : Event(mapping, data) {
    @JsonIgnore
    val thenDo = _thenDo?.eventInstance
}

class OpenSiteService(
    val url: String,
    mapping: String?,
    data: MutableMap<Any?, Any?>?
) : DataService(null, mapping, data)

class RESTService(
    val url: String,
    val method: CallServiceMethod? = CallServiceMethod.get,
    val paginated: PaginatedData? = null,
    @JsonProperty("thenDo") _thenDo: EventObject?,
    mapping: String?,
    data: MutableMap<Any?, Any?>?
) : DataService(_thenDo, mapping, data)

class SaveDataService(
    val key : String,
    @JsonProperty("thenDo") _thenDo: EventObject?,
    mapping: String?,
    data: MutableMap<Any?, Any?>?
) : DataService(_thenDo, mapping, data)

class GetSavedDataService(
    val key : String,
    @JsonProperty("thenDo") _thenDo: EventObject?,
    mapping: String?,
    data: MutableMap<Any?, Any?>?
) : DataService(_thenDo, mapping, data)