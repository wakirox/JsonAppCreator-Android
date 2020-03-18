package it.sapienza.appinterpreter.model.event

import it.sapienza.appinterpreter.model.screen.Screen

class ShowScreen(
    val returnIfStacked : Boolean = false,
    var screen : Screen
) : Event