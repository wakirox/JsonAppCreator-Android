package it.sapienza.appinterpreter.model.view_model.form_model

import it.sapienza.appinterpreter.form.extensions.stringValue
import it.sapienza.appinterpreter.model.view_model.form_model.helpers.FormElementImpl
import java.util.*

class DateInput(
    val min: Date?,
    val max: Date?,
    var value: Date?,
    mandatory: Boolean?,
    title: String,
    description: String?,
    mapping: String
) : FormElementImpl(mandatory, title, description, mapping) {
    override fun isSet(): Boolean = mandatory == false || value != null
    override fun value(): Any? = value?.stringValue()
}