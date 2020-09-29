package it.sapienza.appinterpreter.model

import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.view_model.helper.View
import it.sapienza.appinterpreter.model.view_model.helper.ViewObject

/**
 * ModelApplication comment
 */
class ModelApplication (
    var name : String,
    var author : String?,
    var debugMode : Boolean? = false,
    var version : String,
    var changelog : String?,
    var mainView : ViewObject,
    var views: MutableList<ViewObject> = mutableListOf(),
    var actions : MutableList<ViewObject> = mutableListOf()
){

    fun viewBy(id: String) : View? = views.find { s->s.id == id }

    fun viewBy(view: View) : View? {
        return if(views.isEmpty()) views.find { s->s.id == view.id } else view
    }

    fun actionBy(action : Action) : Action? {
        return if(action.isEmpty()) actions.find { s->s.id == action.id } else action

    }

}