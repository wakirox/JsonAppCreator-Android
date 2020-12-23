package it.sapienza.appinterpreter.model.view_model.form_model

import com.fasterxml.jackson.annotation.JsonFormat
import it.sapienza.appinterpreter.form.extensions.stringValue
import it.sapienza.appinterpreter.model.view_model.form_model.helpers.FormElementImpl
import java.util.*
class DateInput(
    @JsonFormat(shape = JsonFormat.Shape.STRING,pattern = "dd/MM/yyyy") val min: Date?,
    @JsonFormat(shape = JsonFormat.Shape.STRING,pattern = "dd/MM/yyyy") val max: Date?,
    @JsonFormat(shape = JsonFormat.Shape.STRING,pattern = "dd/MM/yyyy") var value: Date?,
    mandatory: Boolean?,
    title: String,
    description: String?,
    mapping: String
) : FormElementImpl(mandatory, title, description, mapping) {
    override fun isSet(): Boolean = mandatory == false || value != null
    override fun value(): Any? = value
}