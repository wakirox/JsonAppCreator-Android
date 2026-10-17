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
    var schemaVersion: Int? = null,
    var author : String?,
    var debugMode : Boolean? = false,
    var version : String,
    var changelog : String?,
    /** One emoji for the app's icon; without it, the icon is the name's first letter. */
    var icon : String? = null,
    @JsonProperty("mainView") mainViewObject : ViewObject,
    @JsonProperty("views") viewObjects: MutableList<ViewObject> = mutableListOf(),
    var actions : MutableList<Action> = mutableListOf()
){

   @JsonIgnore val mainView = mainViewObject.convert()

    @JsonIgnore
    var views : MutableList<MView> = viewObjects.map { it.convert() }.toMutableList()

    fun viewBy(id: String) : MView? = views.find { s->s.id == id }

    fun viewBy(view: MView) : MView? {
        return if(view.isReference()) views.find { it.id == view.referenceId } else view
    }

    fun actionBy(action : Action) : Action? {
        return if(action.isReference()) actions.find { it.id == action.referenceId } else action

    }

}
