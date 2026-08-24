package it.sapienza.appinterpreter.model.view_model

import com.fasterxml.jackson.annotation.JsonProperty
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.view_model.form_model.helpers.FormElementImpl
import it.sapienza.appinterpreter.model.view_model.helper.MView

class FormView(var title : String?,
               var buttonTitle : String?,
               @JsonProperty("formElements") formElementObjects : List<FormObject>,
               id : String?,
               action : Action?,
               mapping : String?,
               data : MutableMap<Any?, Any?>?) : MView(id, action, mapping, data) {

    val formElements : List<FormElementImpl> = formElementObjects.map { it.convert() }

}
