package it.sapienza.appinterpreter.model.view_model

import com.fasterxml.jackson.annotation.JsonProperty
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.view_model.form_model.helpers.FormElement
import it.sapienza.appinterpreter.model.view_model.helper.View
import it.sapienza.appinterpreter.model.view_model.helper.ViewElement

class FormView(var title : String?,
               var buttonTitle : String?,
               @JsonProperty("formElements") val _formElements : List<FormObject>,
               mapping: String?) : View(mapping), ViewElement {

    override var action : Action? = null

    fun isEmpty() = _formElements.isEmpty()

    val formElements : List<FormElement>
        get() = _formElements.map { v->v.convert() }

}
