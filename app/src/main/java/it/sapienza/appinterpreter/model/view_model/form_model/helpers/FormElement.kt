package it.sapienza.appinterpreter.model.view_model.form_model.helpers

abstract class FormElement(
    val title: String,
    val mapping: String
)

abstract class FormElementImpl(
    val mandatory: Boolean? = false,
    title: String,
    val description: String?,
    mapping: String
) : FormElement(title, mapping) {

    /** `mandatory` defaults to false in the schema, so a missing value means optional. */
    val isRequired: Boolean get() = mandatory == true

    abstract fun isSet(): Boolean
    abstract fun value(): Any?

}