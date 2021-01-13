package it.sapienza.appinterpreter.model.event

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import it.sapienza.appinterpreter.model.view_model.ElementType
import java.util.HashMap

class DataEventObject(val type : ElementType.DataType) {
    var eventInstance : DataService? = null
        get() = when(type){
            ElementType.DataType.rest -> {
                val convertValue =
                    jacksonObjectMapper().convertValue<RESTService>(additionalProperties)
                convertValue
            }
            ElementType.DataType.save -> {
                jacksonObjectMapper().convertValue<SaveDataService>(additionalProperties)
            }
            ElementType.DataType.getsaved -> {
                jacksonObjectMapper().convertValue<GetSavedDataService>(additionalProperties)
            }
        }


    @JsonIgnore
    private val additionalProperties: MutableMap<String, Any> =
        HashMap()

    @JsonAnyGetter
    fun getAdditionalProperties(): Map<String, Any> {
        return additionalProperties
    }

    @JsonAnySetter
    fun setAdditionalProperty(name: String, value: Any) {
        additionalProperties[name] = value
    }
}