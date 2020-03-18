package it.sapienza.appinterpreter.model.view_model.form_model.helpers

import it.sapienza.appinterpreter.model.view_model.helper.View

abstract class FormElement(
    val mandatory : Boolean? = false,
    val title : String,
    val description : String?,
    mapping : String
) : View(mapping) {

    abstract fun isSet() : Boolean
    abstract fun value() : Any?

}