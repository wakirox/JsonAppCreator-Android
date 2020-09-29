package it.sapienza.appinterpreter.model.view_model.helper

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import it.sapienza.appinterpreter.model.action.Action
import org.json.JSONObject

open class View(
    var id : String? = null,
    var action : Action? = null,
    var mapping : String? = null,
    var data : MutableMap<Any?, Any?>? = null
){
    var dataObj: JSONObject?
        get() = data?.let {
            JSONObject(it)
        }
        set(value) {
            val typeRef: TypeReference<MutableMap<Any?, Any?>?> =
                object : TypeReference<MutableMap<Any?,Any?>?>() {}
            data = jacksonObjectMapper().readValue(value.toString(), typeRef)
        }

    fun isEmpty() : Boolean{
        return id == null
    }
}