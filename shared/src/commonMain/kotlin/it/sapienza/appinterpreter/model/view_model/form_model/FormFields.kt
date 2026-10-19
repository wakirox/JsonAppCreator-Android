package it.sapienza.appinterpreter.model.view_model.form_model

import it.sapienza.appinterpreter.model.view_model.form_model.helpers.FormElementImpl
import it.sapienza.appinterpreter.model.view_model.form_model.helpers.TextType
import it.sapienza.appinterpreter.utils.FormDates
import kotlinx.serialization.Serializable

@Serializable
class CheckBox(
    val value: Boolean = false,
    override val mandatory: Boolean? = null,
    override val title: String,
    override val description: String? = null,
    override val mapping: String
) : FormElementImpl() {
    override fun isSet(value: Any?): Boolean = !isRequired || value == true
    override fun defaultValue(): Any? = value
}

/** A date field. [min], [max] and [value] are instants in epoch milliseconds, read as Jackson read dates. */
@Serializable
class DateInput(
    @Serializable(with = JacksonDateSerializer::class) val min: Long? = null,
    @Serializable(with = JacksonDateSerializer::class) val max: Long? = null,
    @Serializable(with = JacksonDateSerializer::class) val value: Long? = null,
    override val mandatory: Boolean? = null,
    override val title: String,
    override val description: String? = null,
    override val mapping: String
) : FormElementImpl() {
    override fun isSet(value: Any?): Boolean = !isRequired || !(value as? String).isNullOrBlank()
    override fun defaultValue(): Any? = value?.let(FormDates::format)
}

@Serializable
class Selector(
    val options: List<String>,
    val selection: String? = null,
    override val mandatory: Boolean? = null,
    override val title: String,
    override val description: String? = null,
    override val mapping: String
) : FormElementImpl() {
    override fun isSet(value: Any?): Boolean = !isRequired || (value as? String) in options
    override fun defaultValue(): Any? = selection?.takeIf { it in options }
}

@Serializable
class TextInput(
    val placeholder: String? = null,
    val value: String? = null,
    val textType: TextType? = TextType.plain,
    override val mandatory: Boolean? = null,
    override val title: String,
    override val description: String? = null,
    override val mapping: String
) : FormElementImpl() {
    override fun isSet(value: Any?): Boolean = !isRequired || !(value as? String).isNullOrBlank()
    override fun defaultValue(): Any? = value
}
