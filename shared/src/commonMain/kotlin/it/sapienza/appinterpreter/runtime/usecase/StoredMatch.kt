package it.sapienza.appinterpreter.runtime.usecase

import it.sapienza.appinterpreter.json.displayText
import it.sapienza.appinterpreter.json.getValue
import it.sapienza.appinterpreter.json.matchReplace
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Which saved objects a "where" means: those whose fields have its values, as text, with ${field} placeholders filled
 * from [data]. No "where" (or an empty one) means every object.
 */
class StoredMatch(where: JsonObject?, data: JsonObject?) {
    private val wanted = where.orEmpty().mapValues { (_, value) ->
        if (value is JsonPrimitive && value.isString) value.content.matchReplace(data) else value.displayText()
    }

    fun matches(item: JsonObject): Boolean = wanted.all { (field, value) -> item.getValue(field) == value }
}
