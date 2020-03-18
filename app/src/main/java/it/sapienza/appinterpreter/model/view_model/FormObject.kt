package it.sapienza.appinterpreter.model.view_model

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import it.sapienza.appinterpreter.model.view_model.form_model.CheckBox
import it.sapienza.appinterpreter.model.view_model.form_model.DateInput
import it.sapienza.appinterpreter.model.view_model.form_model.Selector
import it.sapienza.appinterpreter.model.view_model.form_model.TextInput
import it.sapienza.appinterpreter.model.view_model.form_model.helpers.FormElement

class FormObject(var type: ElementType.FormElem) {

    fun convert() : FormElement {
        return when(type){
            ElementType.FormElem.text -> {
                jacksonObjectMapper().convertValue<TextInput>(additionalProperties)
            }
            ElementType.FormElem.selector -> {
                jacksonObjectMapper().convertValue<Selector>(additionalProperties)
            }
            ElementType.FormElem.date -> {
                jacksonObjectMapper().convertValue<DateInput>(additionalProperties)
            }
            ElementType.FormElem.checkbox -> {
                jacksonObjectMapper().convertValue<CheckBox>(additionalProperties)
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