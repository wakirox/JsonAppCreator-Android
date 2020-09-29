package it.sapienza.appinterpreter.model.view_model

import com.fasterxml.jackson.annotation.JsonProperty
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.view_model.form_model.helpers.FormElement
import it.sapienza.appinterpreter.model.view_model.form_model.helpers.FormElementImpl
import it.sapienza.appinterpreter.model.view_model.helper.View

class FormView(var title : String?,
               var buttonTitle : String?,
               @JsonProperty("formElements") val _formElements : List<FormObject>,
               id : String?,
               action : Action?,
               mapping : String?,
               data : MutableMap<Any?, Any?>?) : View(id, action, mapping, data) {

    fun isEmpty() = _formElements.isEmpty()

    val formElements : List<FormElementImpl>
        get() = _formElements.map { v->v.convert() }

}
