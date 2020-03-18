package it.sapienza.appinterpreter.model.event

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import it.sapienza.appinterpreter.model.view_model.ElementType
import it.sapienza.appinterpreter.model.view_model.helper.ViewElement
import java.util.HashMap

class EventObject(val type : ElementType.EventType){

    var eventInstance : Event? = null
        get() = when(type){
            ElementType.EventType.alert -> {
                val convertValue =
                    jacksonObjectMapper().convertValue<AlertMessage>(additionalProperties)

//                convertValue.thenDoOK?.convert()
//                convertValue.thenDoKO?.convert()

                convertValue
            }
            ElementType.EventType.call -> {
                val convertValue =
                    jacksonObjectMapper().convertValue<CallService>(additionalProperties)

//                convertValue.thenDo?.convert()

                convertValue
            }
            ElementType.EventType.screen -> {
                jacksonObjectMapper().convertValue<ShowScreen>(additionalProperties)
            }
        }


    @JsonIgnore
    private val additionalProperties: MutableMap<String, Any> =
        HashMap()

//    fun convert() {
//        eventInstance =   when(type){
//            ElementType.EventType.alert -> {
//                val convertValue =
//                    jacksonObjectMapper().convertValue<AlertMessage>(additionalProperties)
//
//                convertValue.thenDoOK?.convert()
//                convertValue.thenDoKO?.convert()
//
//                convertValue
//            }
//            ElementType.EventType.call -> {
//                val convertValue =
//                    jacksonObjectMapper().convertValue<CallService>(additionalProperties)
//
//                convertValue.thenDo?.convert()
//
//                convertValue
//            }
//            ElementType.EventType.screen -> {
//                jacksonObjectMapper().convertValue<ShowScreen>(additionalProperties)
//            }
//        }
//    }

    @JsonAnyGetter
    fun getAdditionalProperties(): Map<String, Any> {
        return additionalProperties
    }

    @JsonAnySetter
    fun setAdditionalProperty(name: String, value: Any) {
        additionalProperties[name] = value
    }
}