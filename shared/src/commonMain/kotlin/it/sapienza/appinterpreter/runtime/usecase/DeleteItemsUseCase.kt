package it.sapienza.appinterpreter.runtime.usecase

import it.sapienza.appinterpreter.json.getObject
import it.sapienza.appinterpreter.json.getValue
import it.sapienza.appinterpreter.json.matchReplace
import it.sapienza.appinterpreter.model.event.DeleteData
import it.sapienza.appinterpreter.runtime.AppRepository
import it.sapienza.appinterpreter.storage.AppData
import it.sapienza.appinterpreter.utils.AppLibrary
import kotlinx.serialization.json.JsonObject

/** Removes saved objects of the current app: the one the event names, or all those its "where" matches. */
class DeleteItemsUseCase(private val appData: AppData, private val repository: AppRepository) {

    /** How many objects were removed; placeholders are filled from [data] (the part the mapping selects). */
    operator fun invoke(event: DeleteData, data: JsonObject?): Int {
        val source = event.mapping?.let { data?.getObject(it) } ?: data
        val id = event.id.matchReplace(source)
        val match =
            event.where?.let { StoredMatch(it, source)::matches } ?: { item: JsonObject -> item.getValue("id") == id }
        return appData.delete(AppLibrary.idOf(repository.current().name), event.collection, match)
    }
}
