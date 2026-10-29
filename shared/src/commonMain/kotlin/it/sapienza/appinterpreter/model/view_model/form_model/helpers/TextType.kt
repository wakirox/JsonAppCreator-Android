package it.sapienza.appinterpreter.model.view_model.form_model.helpers

import kotlinx.serialization.Serializable

@Serializable
enum class TextType {
    plain,
    numeric,
    email,
    password,

    /** A web address: the keyboard has "/" and ".com". */
    url
}
