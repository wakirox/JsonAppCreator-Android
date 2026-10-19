package it.sapienza.appinterpreter.model.view_model.form_model.helpers

import it.sapienza.appinterpreter.model.TypedSerializer
import it.sapienza.appinterpreter.model.view_model.form_model.CheckBox
import it.sapienza.appinterpreter.model.view_model.form_model.DateInput
import it.sapienza.appinterpreter.model.view_model.form_model.Selector
import it.sapienza.appinterpreter.model.view_model.form_model.TextInput
import kotlinx.serialization.Serializable

object FormElementSerializer : TypedSerializer<FormElementImpl>(
    "FormElement",
    listOf(
        Type("text", TextInput.serializer()) { it is TextInput },
        Type("selector", Selector.serializer()) { it is Selector },
        Type("date", DateInput.serializer()) { it is DateInput },
        Type("checkbox", CheckBox.serializer()) { it is CheckBox }
    )
)

abstract class FormElement {
    abstract val title: String
    abstract val mapping: String
}

/** A form field, chosen by its "type". */
@Serializable(with = FormElementSerializer::class)
abstract class FormElementImpl : FormElement() {
    abstract val mandatory: Boolean?
    abstract val description: String?

    /** `mandatory` defaults to false in the schema, so a missing value means optional. */
    val isRequired: Boolean get() = mandatory == true

    /** Whether [value], what the user entered on screen, satisfies the field: always, for an optional one. */
    abstract fun isSet(value: Any?): Boolean

    /** The value the field starts with when the screen's data doesn't give one. */
    abstract fun defaultValue(): Any?
}
