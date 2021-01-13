package it.sapienza.appinterpreter.model.view_model.helper

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import it.sapienza.appinterpreter.model.action.Action
import org.json.JSONObject

open class MView(
    var id : String? = null,
    var action : Action? = null,
    var mapping : String? = null,
    var data : MutableMap<Any?, Any?>? = null,
    val hideIfAttr : String? = null,
    val viewType : String? = "default"
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

    fun isVisible(obj : JSONObject?) : Boolean {
        hideIfAttr?.let {
            if (it.contains('=')) {
                val split = it.split("=")
                return obj?.isNull(split[0]) == true || obj?.getString(split[0]) != split[1]
            }

            return obj?.isNull(it) != true
        }
        return true
    }

    open fun isEmpty() : Boolean{
        return id != null && (action == null || mapping == null || data == null)
    }

    override fun toString(): String {
        return "MView(id=$id)"
    }


}