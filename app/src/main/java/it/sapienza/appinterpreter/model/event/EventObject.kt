package it.sapienza.appinterpreter.model.event

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import it.sapienza.appinterpreter.model.view_model.ElementType
import java.util.HashMap

class EventObject(val type : ElementType.EventType){

    var eventInstance : Event? = null
        get() = when(type){
            ElementType.EventType.alert -> {
                val convertValue =
                    jacksonObjectMapper().convertValue<AlertMessage>(additionalProperties)
                convertValue
            }
            ElementType.EventType.rest -> {
                val convertValue =
                    jacksonObjectMapper().convertValue<RESTService>(additionalProperties)
                convertValue
            }
            ElementType.EventType.show -> {
                jacksonObjectMapper().convertValue<ShowView>(additionalProperties)
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