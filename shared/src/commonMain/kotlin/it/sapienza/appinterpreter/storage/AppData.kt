package it.sapienza.appinterpreter.storage

import it.sapienza.appinterpreter.json.JsonText
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * What the JSON apps keep on the phone: for each app (by its library id, see AppLibrary.idOf), named collections of
 * objects, in the order they were saved. Used from the main thread, as the screens' events run there.
 */
class AppData(
    private val store: AppStore,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val newId: () -> String = ::randomId,
) {
    /** [collection] of the app [appId] changed. */
    data class Change(val appId: String, val collection: String)

    private val changeFlow = MutableSharedFlow<Change>(extraBufferCapacity = 16)
    val changes: SharedFlow<Change> = changeFlow.asSharedFlow()

    /** The saved objects of [collection], oldest first; none when nothing was saved (or what was can't be read). */
    fun items(appId: String, collection: String): List<JsonObject> =
        store.get(key(appId, collection))?.let { value ->
            runCatching { (JsonText.parse(value) as JsonArray).mapNotNull { it as? JsonObject } }.getOrNull()
        }.orEmpty()

    /** Saves [item] at the end of [collection], with an "id" and "createdAt" (epoch milliseconds) unless it has them. */
    fun add(appId: String, collection: String, item: JsonObject): JsonObject {
        val saved = JsonObject(
            buildMap {
                put("id", JsonPrimitive(newId()))
                put("createdAt", JsonPrimitive(now()))
                putAll(item)
            }
        )
        store.put(key(appId, collection), JsonArray(items(appId, collection) + saved).toString())
        changeFlow.tryEmit(Change(appId, collection))
        return saved
    }

    /** As a JSON array, so no app or collection name can make two keys meet. */
    private fun key(appId: String, collection: String) = JsonArray(listOf(JsonPrimitive(appId), JsonPrimitive(collection))).toString()

    private companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun randomId(): String = Uuid.random().toString()
    }
}
