package it.sapienza.appinterpreter.model.action

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import it.sapienza.appinterpreter.model.event.EventObject
import org.json.JSONObject

class Action(val id : String,
             var data : MutableMap<Any?, Any?>?,
             val type : ActionType = ActionType.click,
             val event : EventObject?){

    fun isEmpty() = event == null
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