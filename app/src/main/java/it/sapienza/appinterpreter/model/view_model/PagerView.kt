package it.sapienza.appinterpreter.model.view_model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.event.DataEventObject
import it.sapienza.appinterpreter.model.event.DataService
import it.sapienza.appinterpreter.model.view_model.helper.MView
import it.sapienza.appinterpreter.model.view_model.helper.ViewObject
import org.json.JSONArray

class PagerView(
     var tabs : List<TabView> = listOf(),
    @JsonProperty("initData") var _initData : DataEventObject?,
    id : String?,
    action : Action?,
    mapping : String?,
    data : MutableMap<Any?, Any?>?
) : MView(id, action, mapping, data){

    val dataObjArr : JSONArray?
        get() = data?.let {
            mapping?.let { key ->
                JSONArray(key)
            }
        }

    var initDataService : DataService? =  _initData?.eventInstance

    override fun isEmpty(): Boolean {
        return super.isEmpty() && tabs.isEmpty()
    }

}