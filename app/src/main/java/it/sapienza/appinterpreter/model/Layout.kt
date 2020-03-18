package it.sapienza.appinterpreter.model

import com.fasterxml.jackson.annotation.JsonProperty
import it.sapienza.appinterpreter.model.view_model.helper.ViewElement
import it.sapienza.appinterpreter.model.view_model.helper.ViewObject
import org.json.JSONObject

class Layout(
    var id : String,
    var orientation: LayoutOrientation? = LayoutOrientation.vertical,
    var data : MutableMap<Any?,Any?>?,
    var inheritedData : Boolean? = false,
    @JsonProperty("views") val _views: List<ViewObject> = listOf()
){

    fun isEmpty() = _views.isEmpty()


    val views : List<ViewElement>
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