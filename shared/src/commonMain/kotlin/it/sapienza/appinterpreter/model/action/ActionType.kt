package it.sapienza.appinterpreter.model.action

import kotlinx.serialization.Serializable

@Serializable
enum class ActionType {
    click,
    longclick
}
