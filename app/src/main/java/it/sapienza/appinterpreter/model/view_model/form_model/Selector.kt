package it.sapienza.appinterpreter.model.view_model.form_model

import it.sapienza.appinterpreter.form.extensions.stringValue
import it.sapienza.appinterpreter.model.view_model.form_model.helpers.FormElement

class Selector(
    var options : List<String>,
    var selection : String?,
    mandatory: Boolean?,
    title: String,
    description: String?,
    mapping: String
) : FormElement(mandatory, title, description, mapping){
    override fun isSet(): Boolean =  mandatory == false || selection != null
    override fun value(): Any? = selection
}