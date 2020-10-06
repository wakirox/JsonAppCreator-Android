package it.sapienza.appinterpreter.model.action

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import it.sapienza.appinterpreter.model.event.EventObject
import org.json.JSONObject

class Action(val id : String,
             var data : MutableMap<Any?, Any?>?,
             val type : ActionType = ActionType.click,
             @JsonProperty("event") val _event : EventObject?){

    fun isEmpty() = _event == null

    @JsonIgnore val event = _event?.eventInstance

//    fun convert(){
//        event!!.convert()
//    }

    var dataObj: JSONObject?
        get() = data?.let { JSONObject(it) }
        set(value) {
            val typeRef: TypeReference<MutableMap<Any?, Any?>?> =
                object : TypeReference<MutableMap<Any?, Any?>?>() {}
            data = jacksonObjectMapper().readValue(value.toString(), typeRef)
        }

}