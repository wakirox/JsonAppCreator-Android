package it.sapienza.appinterpreter.model.view_model

import it.sapienza.appinterpreter.model.Layout
import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.event.InitService
import it.sapienza.appinterpreter.model.view_model.helper.View
import it.sapienza.appinterpreter.model.view_model.helper.ViewElement
import org.json.JSONArray

class ListView(
    var data : List<MutableMap<Any?,Any?>>?,
    var layout : Layout,
    mapping : String?,
    var init : InitService?,
    var orientation: LayoutOrientation = LayoutOrientation.vertical
) : ViewElement, View(mapping){
    override var action: Action? = null

    val dataObj: JSONArray?
        get() = data?.let {
            JSONArray(it)
        }

}