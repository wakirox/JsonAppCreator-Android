package it.sapienza.appinterpreter.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.view_model.helper.MView
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
    @JsonProperty("mainView") mainViewObject : ViewObject,
    @JsonProperty("views") viewObjects: MutableList<ViewObject> = mutableListOf(),
    var actions : MutableList<Action> = mutableListOf()
){

   @JsonIgnore val mainView = mainViewObject.convert()

    @JsonIgnore
    var views : MutableList<MView> = viewObjects.map { it.convert() }.toMutableList()

    fun viewBy(id: String) : MView? = views.find { s->s.id == id }

    fun viewBy(view: MView) : MView? {
        return if(view.isEmpty()) views.find { s->s.id == view.id } else view
    }

    fun actionBy(action : Action) : Action? {
        return if(action.isEmpty()) actions.find { s->s.id == action.id } else action

    }

}
