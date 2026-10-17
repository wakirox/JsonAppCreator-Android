package it.sapienza.appinterpreter.extensions

import org.json.JSONArray
import org.json.JSONObject

/**
 * The value at a dot path such as "owner.name". Array items are reached by index, as "meals.0.strMeal" or
 * "meals[0].strMeal". Null when a step is missing.
 */
fun JSONObject.valueAt(mapping: String): Any? {
    var current: Any? = this
    for (step in mapping.replace("[", ".").replace("]", "").split('.').filter { it.isNotEmpty() }) {
        current = when (current) {
            is JSONObject -> current.opt(step)
            is JSONArray -> step.toIntOrNull()?.let { current.opt(it) }
            else -> null
        }
        if (current == null || current == JSONObject.NULL) return null
    }
    return current
}

fun JSONObject.getValue(mapping : String) : String? = when (val value = valueAt(mapping)) {
    null -> null
    is JSONArray -> (0 until value.length()).joinToString(", ") { index -> value.get(index).toString() }
    else -> value.toString()
}

fun JSONObject.getArray(mapping : String) : JSONArray? = valueAt(mapping) as? JSONArray

/** The object at a path, such as the one item of a REST response with "meals.0". */
fun JSONObject.getObject(mapping: String): JSONObject? = valueAt(mapping) as? JSONObject
