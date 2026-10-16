package it.sapienza.appinterpreter.model.view_model.form_model

import it.sapienza.appinterpreter.model.view_model.form_model.helpers.FormElementImpl

class Selector(
    var options : List<String>,
    var selection : String?,
    mandatory: Boolean?,
    title: String,
    description: String?,
    mapping: String
) : FormElementImpl(mandatory, title, description, mapping){
    override fun isSet(value: Any?): Boolean = !isRequired || (value as? String) in options
    override fun defaultValue(): Any? = selection?.takeIf { it in options }
}