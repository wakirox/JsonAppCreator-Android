package it.sapienza.appinterpreter.model.view_model.form_model

import it.sapienza.appinterpreter.model.view_model.form_model.helpers.FormElement
import it.sapienza.appinterpreter.model.view_model.form_model.helpers.TextType

class TextInput(
    var placeholder: String?,
    var value: String?,
    var textType: TextType? = TextType.plain,
    mandatory: Boolean?,
    title: String,
    description: String?,
    mapping: String
) : FormElement(mandatory, title, description, mapping) {
    override fun isSet(): Boolean =  mandatory == false || value != null
    override fun value(): Any? = value //TODO fare in maniera tale da gestire i numeri
}