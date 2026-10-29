package it.sapienza.appinterpreter.runtime.usecase

import it.sapienza.appinterpreter.json.valueAt
import it.sapienza.appinterpreter.model.view_model.StoredData
import it.sapienza.appinterpreter.runtime.AppRepository
import it.sapienza.appinterpreter.runtime.ListChoice
import it.sapienza.appinterpreter.storage.AppData
import it.sapienza.appinterpreter.utils.AppLibrary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * The current app's saved objects a list shows: those of its collection that match its "where", filled from [data],
 * in its "sort" order. Its "initialItems" are saved first, the first time.
 */
class LoadStoredItemsUseCase(private val appData: AppData, private val repository: AppRepository) {

    /** A stored list's items as [choice] orders and filters them, and the tags its items use (for the filter chips). */
    class Result(val items: List<JsonObject>, val tags: List<String>)

    operator fun invoke(stored: StoredData, data: JsonObject?): List<JsonObject> = load(stored, data).items

    fun load(stored: StoredData, data: JsonObject?, choice: ListChoice = ListChoice()): Result {
        val match = StoredMatch(stored.where, data)
        stored.initialItems?.let { appData.addFirst(currentAppId(), stored.collection, it, stored.unique) }
        val matching = appData.items(currentAppId(), stored.collection).filter(match::matches)
        val tags = stored.filterBy?.let { field -> tagsIn(matching, field) }.orEmpty()
        val shown = choice.tag?.takeIf { stored.filterBy != null }
            ?.let { tag ->
                matching.filter { item -> tagsOf(item, stored.filterBy!!).any { it.equals(tag, ignoreCase = true) } }
            }
            ?: matching
        val sort =
            stored.sortOptions?.getOrNull(choice.sortOption)?.sort ?: stored.sortOptions?.firstOrNull()?.sort
                ?: stored.sort
        return Result(sorted(shown, sort), tags)
    }

    /** The collections of the current app that changed, so lists showing them load again. */
    val changes: Flow<String>
        get() = appData.changes.filter { it.appId == currentAppId() }.map { it.collection }

    private fun currentAppId() = AppLibrary.idOf(repository.current().name)

    companion object {
        /** An item's tags at [field]: a list's texts, or text split at commas; trimmed, empty ones left out. */
        fun tagsOf(item: JsonObject, field: String): List<String> = when (val value = item.valueAt(field)) {
            is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            is JsonPrimitive -> value.contentOrNull?.split(',').orEmpty()
            else -> emptyList()
        }.map(String::trim).filter(String::isNotEmpty)

        /** Every tag [items] use at [field], each once (whatever its case), alphabetically. */
        fun tagsIn(items: List<JsonObject>, field: String): List<String> =
            items.flatMap { tagsOf(it, field) }.distinctBy(String::lowercase).sortedBy(String::lowercase)
    }
}
