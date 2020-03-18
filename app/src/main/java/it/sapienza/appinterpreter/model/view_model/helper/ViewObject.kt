package it.sapienza.appinterpreter.model.view_model.helper

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import it.sapienza.appinterpreter.model.view_model.*

import java.util.HashMap


class ViewObject(var type: ElementType.ViewElem) {

    fun convert() : ViewElement {
        return when(type){
            ElementType.ViewElem.text -> {
                jacksonObjectMapper().convertValue<TextView>(additionalProperties)
            }
            ElementType.ViewElem.image -> {
                jacksonObjectMapper().convertValue<ImageView>(additionalProperties)
            }
            ElementType.ViewElem.button -> {
                jacksonObjectMapper().convertValue<ButtonView>(additionalProperties)
            }
            ElementType.ViewElem.form -> {
                    jacksonObjectMapper().convertValue<FormView>(additionalProperties)
            }
            ElementType.ViewElem.list -> {
                jacksonObjectMapper().convertValue<ListView>(additionalProperties)
            }
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