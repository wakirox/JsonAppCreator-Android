package it.sapienza.appinterpreter.model.view_model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.event.DataEventObject
import it.sapienza.appinterpreter.model.event.DataService
import it.sapienza.appinterpreter.model.view_model.form_model.helpers.FormElementImpl
import it.sapienza.appinterpreter.model.view_model.helper.MView

class FormView(var title : String?,
               var buttonTitle : String?,
               @JsonProperty("formElements") val _formElements : List<FormObject>,
               @JsonProperty("initData") var _initData : DataEventObject?,
               id : String?,
               action : Action?,
               mapping : String?,
               data : MutableMap<Any?, Any?>?) : MView(id, action, mapping, data) {

    override fun isEmpty() = _formElements.isEmpty()

    @JsonIgnore
    val formElements : List<FormElementImpl>  = _formElements.map { v->v.convert() }

    var initDataService : DataService? =  _initData?.eventInstance

}
