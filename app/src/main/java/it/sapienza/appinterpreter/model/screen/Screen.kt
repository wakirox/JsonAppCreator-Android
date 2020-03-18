package it.sapienza.appinterpreter.model.screen

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import it.sapienza.appinterpreter.model.Layout
import it.sapienza.appinterpreter.model.event.CallService
import org.json.JSONObject

class Screen(
    var id : String,
    var data : MutableMap<Any?,Any?>?,
    var inheritedData : Boolean? = true,
    var init : CallService?,
    var layouts : List<Layout> = listOf()){
    fun isEmpty() = layouts.isEmpty()

    var dataObj: JSONObject?
        get() = data?.let { JSONObject(it) }
        set(value) {
            val typeRef: TypeReference<MutableMap<Any?,Any?>?> =
                object : TypeReference<MutableMap<Any?,Any?>?>() {}

            data = jacksonObjectMapper().readValue(value.toString(), typeRef)
        }

    fun toReference() : Screen {
        return Screen(id,null,null, null,listOf())
    }

}