package it.sapienza.appinterpreter.runtime.usecase

import it.sapienza.appinterpreter.model.view_model.StoredData
import it.sapienza.appinterpreter.runtime.AppRepository
import it.sapienza.appinterpreter.storage.AppData
import it.sapienza.appinterpreter.utils.AppLibrary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject

/**
 * The current app's saved objects a list shows: those of its collection that match its "where", filled from [data],
 * in its "sort" order.
 */
class LoadStoredItemsUseCase(private val appData: AppData, private val repository: AppRepository) {

    operator fun invoke(stored: StoredData, data: JsonObject?): List<JsonObject> {
        val match = StoredMatch(stored.where, data)
        return sorted(appData.items(currentAppId(), stored.collection).filter(match::matches), stored.sort)
    }

    /** The collections of the current app that changed, so lists showing them load again. */
    val changes: Flow<String>
        get() = appData.changes.filter { it.appId == currentAppId() }.map { it.collection }

    private fun currentAppId() = AppLibrary.idOf(repository.current().name)
}
