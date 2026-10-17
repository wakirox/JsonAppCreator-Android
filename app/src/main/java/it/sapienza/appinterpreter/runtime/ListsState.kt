package it.sapienza.appinterpreter.runtime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import it.sapienza.appinterpreter.model.view_model.ListView
import it.sapienza.appinterpreter.runtime.usecase.LoadListItemsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject

/** A screen's lists, each loaded once in [scope]: cancelling the scope cancels their requests. */
class ListsState(private val scope: CoroutineScope, private val loadItems: LoadListItemsUseCase) {

    class Entry(internal val view: ListView, internal val data: JSONObject?) {
        val items = mutableStateListOf<JSONObject>()
        var waiting by mutableStateOf(view.initData != null)
            internal set
        var errorMessage by mutableStateOf<String?>(null)
            internal set
        internal var job: Job? = null
    }

    private val lists = mutableMapOf<String, Entry>()

    fun get(view: ListView, data: JSONObject?): Entry =
        lists.getOrPut("${System.identityHashCode(view)}:${data?.toString().hashCode()}") {
            Entry(view, data).also(::load)
        }

    fun retry(list: Entry) = load(list)

    /** Back on screen: lists that failed try again, since the connection may be back. */
    fun retryFailed() = lists.values.filter { it.errorMessage != null }.forEach(::load)

    private fun load(list: Entry) {
        list.job?.cancel()
        list.errorMessage = null
        list.waiting = list.view.initData != null
        list.job = scope.launch {
            try {
                val items = loadItems(list.view, list.data)
                list.items.clear()
                list.items.addAll(items)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                list.items.clear()
                list.errorMessage = loadItems.errorMessage(e.message ?: "An error occurred")
            }
            list.waiting = false
        }
    }
}
