package it.sapienza.appinterpreter.runtime.usecase

import it.sapienza.appinterpreter.json.JsonText
import it.sapienza.appinterpreter.json.getArray
import it.sapienza.appinterpreter.model.view_model.ListView
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/** A list's items: what the app saved, or from its request when it has one, else from its own data or the screen's. */
class LoadListItemsUseCase(private val fetchJson: FetchJsonUseCase, private val loadStored: LoadStoredItemsUseCase) {

    /** The saved collections that changed: the lists showing them load again. */
    val storedChanges: Flow<String> get() = loadStored.changes

    suspend operator fun invoke(list: ListView, data: JsonObject?): List<JsonObject> {
        list.stored?.let { return loadStored(it, data) }
        val source = list.initData?.let { fetchJson(it, data) } ?: data ?: JsonObject(emptyMap())
        val array = list.mapping?.let { source.getArray(it) } ?: legacyArray(list) ?: return emptyList()
        return array.mapIndexed { index, item ->
            item as? JsonObject ?: throw IllegalArgumentException("Value $item at $index cannot be converted to JSONObject")
        }
    }

    /**
     * What the Android code always did when the mapping found no array but the list has its own data: read the
     * mapping itself as a JSON array, which for a path such as "books" fails with org.json's message.
     */
    private fun legacyArray(list: ListView): JsonArray? {
        if (list.data == null) return null
        val mapping = list.mapping ?: return null
        return runCatching { JsonText.parse(mapping) as? JsonArray }.getOrNull()
            ?: throw IllegalArgumentException("Value $mapping of type java.lang.String cannot be converted to JSONArray")
    }

    /** What the user reads when loading fails with [message]. */
    fun errorMessage(message: String): String = when {
        message.contains("resolve host", ignoreCase = true) ||
            message.contains("failed to connect", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) ->
            "Unable to load data. Check your connection and try again."
        else -> "Unable to load data. $message"
    }
}
