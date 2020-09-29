package it.sapienza.appinterpreter.model

import com.fasterxml.jackson.annotation.JsonProperty
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
    @JsonProperty("mainView") var _mainView : ViewObject,
    @JsonProperty("views") var _views: MutableList<ViewObject> = mutableListOf(),
    var actions : MutableList<Action> = mutableListOf()
){

    val mainView : View
        get() = _mainView.convert()

    val views : List<View>
        get() = _views.map { v->v.convert() }

    fun viewBy(id: String) : View? = views.find { s->s.id == id }

    fun viewBy(view: View) : View? {
        return if(views.isEmpty()) views.find { s->s.id == view.id } else view
    }

    fun actionBy(action : Action) : Action? {
        return if(action.isEmpty()) actions.find { s->s.id == action.id } else action

    }

}