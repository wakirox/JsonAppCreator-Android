package it.sapienza.appinterpreter.model.action

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import it.sapienza.appinterpreter.model.event.EventObject
import org.json.JSONObject

class Action(val id : String = "",
             val ref : String? = null,
             var data : MutableMap<Any?, Any?>?,
             val type : ActionType = ActionType.click,
             @JsonProperty("event") eventObject : EventObject?){

    private val reference = ref != null || eventObject == null

    @get:JsonIgnore
    val referenceId: String
        get() = ref ?: id

    @JsonIgnore
    fun isReference() = reference

    /** Kept for callers compiled against the old model API. */
    fun isEmpty() = isReference()

    @JsonIgnore val event = eventObject?.eventInstance

    var dataObj: JSONObject?
        get() = data?.let { JSONObject(it) }
        set(value) {
            val typeRef: TypeReference<MutableMap<Any?, Any?>?> =
                object : TypeReference<MutableMap<Any?, Any?>?>() {}
            data = jacksonObjectMapper().readValue(value.toString(), typeRef)
        }

}
