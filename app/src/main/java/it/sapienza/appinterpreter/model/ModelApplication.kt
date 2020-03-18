package it.sapienza.appinterpreter.model

import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.event.CallService
import it.sapienza.appinterpreter.model.screen.Screen

/**
 * ModelApplication comment
 */
class ModelApplication (
    var name : String,
    var author : String?,
    var debugMode : Boolean? = false,
    var version : String,
    var previousVersion : String?,
    var changelog : String?,
    var main : Screen,
    var screens : MutableList<Screen> = mutableListOf(),
    var layouts: MutableList<Layout> = mutableListOf(),
    var actions : MutableList<Action> = mutableListOf()
){

    fun screenById(id: String) : Screen? = screens.find { s->s.id == id }

    fun screenBy(screen: Screen) : Screen? {
        return if(screen.isEmpty()) screens.find { s->s.id == screen.id } else screen
    }

    fun layoutBy(layout: Layout) : Layout? {
        return if(layout.isEmpty()) layouts.find { s->s.id == layout.id } else layout
    }

    fun actionBy(action : Action) : Action? {
        return if(action.isEmpty()) actions.find { s->s.id == action.id } else action

    }

}