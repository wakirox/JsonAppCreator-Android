package it.sapienza.appinterpreter.model.view_model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import it.sapienza.appinterpreter.model.view_model.helper.ViewObject
import org.json.JSONObject

class TabView(val name : String,
              @JsonProperty("itemView") var _itemView : ViewObject?,
              val mapping : String?,
              var data : MutableMap<Any?, Any?>?) {

    @JsonIgnore
    var itemView = _itemView!!.convert()

    var dataObj: JSONObject?
        get() = data?.let {
            JSONObject(it)
        }
        set(value) {
            val typeRef: TypeReference<MutableMap<Any?, Any?>?> =
                object : TypeReference<MutableMap<Any?, Any?>?>() {}
            data = jacksonObjectMapper().readValue(value.toString(), typeRef)
        }

}