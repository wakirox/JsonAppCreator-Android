package it.sapienza.appinterpreter.model

import com.fasterxml.jackson.annotation.JsonProperty
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.view_model.helper.View
import it.sapienza.appinterpreter.model.view_model.helper.ViewObject
import org.json.JSONObject

class Layout(
    id : String?,
    action : Action?,
    mapping : String?,
    data : MutableMap<Any?, Any?>?,
    var orientation: LayoutOrientation? = LayoutOrientation.vertical,
    @JsonProperty("views") val _views: List<ViewObject> = listOf()
) : View(id, action, mapping, data){

    fun isEmpty() = _views.isEmpty()

    val views : List<View>
        get() = _views.map { v->v.convert() }

//    fun convert(){
//        convertedViews.clear()
//        convertedViews.addAll(views.map { v->v.convert() })
//    }

    val dataObj: JSONObject?
        get() = data?.let {
            JSONObject(it)
        }


}