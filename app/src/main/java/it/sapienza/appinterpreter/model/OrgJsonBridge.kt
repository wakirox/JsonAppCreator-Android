package it.sapienza.appinterpreter.model

import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.view_model.ListView
import it.sapienza.appinterpreter.model.view_model.helper.MView
import org.json.JSONArray
import org.json.JSONObject

/** A view's data as the org.json object the Android runtime still works with. */
val MView.dataObj: JSONObject?
    get() = data?.let { JSONObject(it.toString()) }

/** An action's data as the org.json object the Android runtime still works with. */
val Action.dataObj: JSONObject?
    get() = data?.let { JSONObject(it.toString()) }

/** As before: with data, the mapping read as a JSON array text (which throws for a plain path). */
val ListView.dataObjArr: JSONArray?
    get() = data?.let { mapping?.let { key -> JSONArray(key) } }
