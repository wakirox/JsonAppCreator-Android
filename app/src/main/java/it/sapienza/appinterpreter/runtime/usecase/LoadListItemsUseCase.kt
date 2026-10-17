package it.sapienza.appinterpreter.runtime.usecase

import it.sapienza.appinterpreter.extensions.getArray
import it.sapienza.appinterpreter.model.view_model.ListView
import org.json.JSONObject

/** A list's items: from its request when it has one, else from its own data or the screen's. */
class LoadListItemsUseCase(private val fetchJson: FetchJsonUseCase) {

    suspend operator fun invoke(list: ListView, data: JSONObject?): List<JSONObject> {
        val source = list.initData?.let { fetchJson(it, data) } ?: data ?: JSONObject()
        val array = list.mapping?.let { source.getArray(it) } ?: list.dataObjArr ?: return emptyList()
        return (0 until array.length()).map(array::getJSONObject)
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
