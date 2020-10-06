package it.sapienza.appinterpreter.model.view_model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.event.RESTService
import it.sapienza.appinterpreter.model.view_model.helper.MView
import it.sapienza.appinterpreter.model.view_model.helper.ViewObject
import org.json.JSONArray

class ListView(
    @JsonProperty("itemView") var _itemView : ViewObject,
    var initData : RESTService?,
    var orientation: LayoutOrientation = LayoutOrientation.vertical,
    id : String?,
    action : Action?,
    mapping : String?,
    data : MutableMap<Any?, Any?>?
) : MView(id, action, mapping, data){

    @JsonIgnore var itemView = _itemView.convert()

    val dataObjArr : JSONArray?
        get() = data?.let {
             mapping?.let { key ->
                JSONArray(key)
            }
        }

}