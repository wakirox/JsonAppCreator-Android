package it.sapienza.appinterpreter.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.action.SearchAction
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
    var params : ModelParams?,
    var searchable : SearchAction?,
    @JsonProperty("mainView") var _mainView : ViewObject,
    @JsonProperty("views") var _views: MutableList<ViewObject> = mutableListOf(),
    var actions : MutableList<Action> = mutableListOf()
){

   @JsonIgnore val mainView = _mainView.convert()

    @JsonIgnore
    var views : MutableList<MView>  = _views.map { v->v.convert() }.toMutableList()

    fun viewBy(id: String) : MView? = views.find { s->s.id == id }

    fun viewBy(view: MView) : MView? {
        return if(view.isEmpty()) views.find { s->s.id == view.id } else view
    }

    fun actionBy(action : Action) : Action? {
        return if(action.isEmpty()) actions.find { s->s.id == action.id } else action

    }

}