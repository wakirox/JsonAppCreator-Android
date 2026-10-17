package it.sapienza.appinterpreter.model.view_model.form_model

import it.sapienza.appinterpreter.model.view_model.form_model.helpers.FormElementImpl
import it.sapienza.appinterpreter.model.view_model.form_model.helpers.TextType

class TextInput(
    var placeholder: String?,
    var value: String?,
    var textType: TextType? = TextType.plain,
    mandatory: Boolean?,
    title: String,
    description: String?,
    mapping: String
) : FormElementImpl(mandatory, title, description, mapping) {
    override fun isSet(value: Any?): Boolean = !isRequired || !(value as? String).isNullOrBlank()
    override fun defaultValue(): Any? = value
}