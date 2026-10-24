package it.sapienza.appinterpreter.runtime.usecase

import it.sapienza.appinterpreter.json.getObject
import it.sapienza.appinterpreter.json.getValue
import it.sapienza.appinterpreter.json.matchReplace
import it.sapienza.appinterpreter.model.event.SaveData
import it.sapienza.appinterpreter.runtime.AppRepository
import it.sapienza.appinterpreter.storage.AppData
import it.sapienza.appinterpreter.utils.AppLibrary
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Saves an object in the current app's collection on the phone, and gives it back with its id. */
class SaveItemUseCase(private val appData: AppData, private val repository: AppRepository) {

    /**
     * [values] (the part the mapping selects) with the event's fields, filled from [context] and then [values]; with the
     * event's unique field, it replaces an object saved with the same value there.
     */
    operator fun invoke(event: SaveData, values: JsonObject?, context: JsonObject?): JsonObject {
        val item = event.mapping?.let { values?.getObject(it) } ?: values ?: JsonObject(emptyMap())
        val fields = event.fields.orEmpty().mapValues { (_, value) ->
            if (value is JsonPrimitive && value.isString) JsonPrimitive(value.content.matchReplace(context).matchReplace(values)) else value
        }
        val appId = AppLibrary.idOf(repository.current().name)
        val saved = JsonObject(item + fields)
        val same = event.unique?.let { field -> saved.getValue(field)?.let { value -> field to value } }
        val existing = same?.let { (field, value) -> appData.items(appId, event.collection).firstOrNull { it.getValue(field) == value } }
        val id = existing?.getValue("id")
        return id?.let { appData.update(appId, event.collection, it, saved) } ?: appData.add(appId, event.collection, saved)
    }
}
