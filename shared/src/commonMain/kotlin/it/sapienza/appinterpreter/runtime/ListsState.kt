package it.sapienza.appinterpreter.runtime

import it.sapienza.appinterpreter.model.view_model.ListView
import it.sapienza.appinterpreter.runtime.usecase.LoadListItemsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

/**
 * A screen's lists, each loaded once in [scope]: cancelling the scope cancels their requests. A list of saved data
 * loads again whenever its collection changes, also while its screen is behind another.
 */
class ListsState(private val scope: CoroutineScope, private val loadItems: LoadListItemsUseCase) {

    /** What a list shows: its items, a wait while they load, or why they couldn't. */
    data class Content(val items: List<JsonObject>, val waiting: Boolean, val errorMessage: String?)

    class Entry internal constructor(internal val view: ListView, internal val data: JsonObject?) {
        internal val contentFlow = MutableStateFlow(Content(emptyList(), waiting = view.initData != null, errorMessage = null))
        val content: StateFlow<Content> = contentFlow.asStateFlow()
        internal var job: Job? = null
    }

    /** By the list definition itself (its identity) and the data it shows. */
    private class Key(val view: ListView, val data: String?) {
        override fun equals(other: Any?) = other is Key && other.view === view && other.data == data
        override fun hashCode() = view.hashCode() * 31 + data.hashCode()
    }

    private val lists = mutableMapOf<Key, Entry>()

    init {
        scope.launch {
            loadItems.storedChanges.collect { collection ->
                lists.values.filter { it.view.stored?.collection == collection }.forEach(::load)
            }
        }
    }

    fun get(view: ListView, data: JsonObject?): Entry =
        lists.getOrPut(Key(view, data?.toString())) { Entry(view, data).also(::load) }

    fun retry(list: Entry) = load(list)

    /** Back on screen: lists that failed try again, since the connection may be back. */
    fun retryFailed() = lists.values.filter { it.content.value.errorMessage != null }.forEach(::load)

    private fun load(list: Entry) {
        list.job?.cancel()
        list.contentFlow.update { it.copy(errorMessage = null, waiting = list.view.initData != null) }
        list.job = scope.launch {
            try {
                val items = loadItems(list.view, list.data)
                list.contentFlow.update { it.copy(items = items) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                list.contentFlow.update { it.copy(items = emptyList(), errorMessage = loadItems.errorMessage(e.message ?: "An error occurred")) }
            }
            list.contentFlow.update { it.copy(waiting = false) }
        }
    }
}
