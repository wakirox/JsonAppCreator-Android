package it.sapienza.appinterpreter.extensions

import org.json.JSONArray
import org.json.JSONObject

fun JSONObject.getValue(mapping : String) : String? {
    if(mapping.contains(".")){
        val firstKey = mapping.split(".").first()
        if(!has(firstKey) || isNull(firstKey)) { return null }

        val jsonObject = getJSONObject(firstKey)
        val key = mapping.substring(mapping.indexOf(".")+1)
        return if(jsonObject.has(key)){
            jsonObject.getValue(key)
        }else{
            null
        }
    }else{
        if(!has(mapping) || isNull(mapping)) { return null }
        return get(mapping).toString()
    }
}

fun JSONObject.getArray(mapping : String) : JSONArray? {
    if(mapping.contains(".")){
        val firstKey = mapping.split(".").first()
        if(!has(firstKey) || isNull(firstKey)) { return null }
        return getJSONObject(firstKey).getArray(mapping.substring(mapping.indexOf(".")+1))
    }else{
        if(!has(mapping) || isNull(mapping)) {
            return null
        }
        return getJSONArray(mapping)
    }
}