package it.sapienza.appinterpreter.model.view_model

import com.fasterxml.jackson.annotation.JsonProperty
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.view_model.form_model.helpers.FormElementImpl
import it.sapienza.appinterpreter.model.view_model.helper.MView

class FormView(var title : String?,
               var buttonTitle : String?,
               @JsonProperty("formElements") val _formElements : List<FormObject>,
               id : String?,
               action : Action?,
               mapping : String?,
               data : MutableMap<Any?, Any?>?) : MView(id, action, mapping, data) {

    override fun isEmpty() = _formElements.isEmpty()

    val formElements : List<FormElementImpl>
        get() = _formElements.map { v->v.convert() }

}
