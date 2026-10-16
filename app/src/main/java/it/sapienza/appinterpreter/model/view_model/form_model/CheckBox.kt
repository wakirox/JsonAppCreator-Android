package it.sapienza.appinterpreter.model.view_model.form_model

import it.sapienza.appinterpreter.model.view_model.form_model.helpers.FormElementImpl

class CheckBox(
    var value : Boolean = false,
    mandatory: Boolean?,
    title: String,
    description: String?,
    mapping: String
) : FormElementImpl(mandatory, title, description, mapping){

    override fun isSet(value: Any?): Boolean = !isRequired || value == true
    override fun defaultValue(): Any? = value

}