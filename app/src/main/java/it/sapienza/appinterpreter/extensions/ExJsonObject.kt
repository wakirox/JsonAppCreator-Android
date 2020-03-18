package it.sapienza.appinterpreter.extensions

import org.json.JSONArray
import org.json.JSONObject

fun JSONObject.getValue(mapping : String) : String? {
    if(mapping.contains(".")){
        val firstKey = mapping.split(".").first()
        if(!has(firstKey)) { return null }
        return getJSONObject(firstKey).getValue(mapping.substring(mapping.indexOf(".")+1))
    }else{
        if(!has(mapping)) { return null }
        return get(mapping).toString()
    }
}

fun JSONObject.getArray(mapping : String) : JSONArray? {
    if(mapping.contains(".")){
        val firstKey = mapping.split(".").first()
        if(!has(firstKey)) { return null }
        return getJSONObject(firstKey).getArray(mapping.substring(mapping.indexOf(".")+1))
    }else{
        if(!has(mapping)) {
            return null
        }
        return getJSONArray(mapping)
    }
}