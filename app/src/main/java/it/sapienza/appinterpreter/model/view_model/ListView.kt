package it.sapienza.appinterpreter.model.view_model

import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.event.RESTService
import it.sapienza.appinterpreter.model.view_model.helper.View
import org.json.JSONArray

class ListView(
    var itemView : View,
    var initData : RESTService?,
    var orientation: LayoutOrientation = LayoutOrientation.vertical,
    id : String?,
    action : Action?,
    mapping : String?,
    data : MutableMap<Any?, Any?>?
) : View(id, action, mapping, data){

    val dataObj: JSONArray?
        get() = data?.let {
             mapping?.let { key ->
                JSONArray(key)
            }
        }

}