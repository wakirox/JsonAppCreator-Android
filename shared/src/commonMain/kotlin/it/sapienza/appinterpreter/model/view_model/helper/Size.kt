package it.sapienza.appinterpreter.model.view_model.helper

import it.sapienza.appinterpreter.model.event.LenientIntSerializer
import kotlinx.serialization.Serializable

@Serializable
class Size(
    @Serializable(with = LenientIntSerializer::class) val width: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val height: Int? = null
)
