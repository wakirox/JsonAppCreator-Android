package it.sapienza.appinterpreter.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.event.DataEventObject
import it.sapienza.appinterpreter.model.event.DataService
import it.sapienza.appinterpreter.model.view_model.helper.MView
import it.sapienza.appinterpreter.model.view_model.helper.ViewObject
import org.json.JSONObject

class Layout(
    id : String? = null,
    action : Action? = null,
    mapping : String? = null,
    data : MutableMap<Any?, Any?>? = null,
    @JsonProperty("initData") var _initData : DataEventObject? = null,
    var orientation: LayoutOrientation? = LayoutOrientation.vertical,
    @JsonProperty("views") val _views: List<ViewObject> = listOf()
) : MView(id, action, mapping, data){

    override fun isEmpty() = _views.isEmpty()


   @JsonIgnore
   var views : MutableList<MView> = _views.map { v->v.convert() }.toMutableList()

    var initDataService : DataService? =  _initData?.eventInstance

//    fun convert(){
//        convertedViews.clear()
//        convertedViews.addAll(views.map { v->v.convert() })
//    }

//    val dataObj: JSONObject?
//        get() = data?.let {
//            JSONObject(it)
//        }


}