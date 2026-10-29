package it.sapienza.appinterpreter.storage

import it.sapienza.appinterpreter.json.JsonText
import it.sapienza.appinterpreter.json.getValue
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
        val saved = stamped(item)
        save(appId, collection, items(appId, collection) + saved)
        return saved
    }

    private fun stamped(item: JsonObject) = JsonObject(
        buildMap {
            put("id", JsonPrimitive(newId()))
            put("createdAt", JsonPrimitive(now()))
            putAll(item)
        }
    )

    /**
     * Saves [items] in [collection], each as [add] would, unless something was ever saved there (even if it was all
     * deleted since); says whether it saved any. With [unique], a field that tells them apart, it also adds the items
     * whose value there it never added before and isn't saved: those a later version of the app brings. An item the
     * user deleted doesn't come back, and saved ones stay as they are.
     */
    fun addFirst(appId: String, collection: String, items: List<JsonObject>, unique: String? = null): Boolean {
        val key = key(appId, collection)
        val never = !store.contains(key)
        val addedKey = key(appId, collection, ADDED)
        val added = unique?.let { readTexts(addedKey) }.orEmpty()
        val saved = items(appId, collection)
        val present = unique?.let { field -> saved.mapNotNull { it.getValue(field) }.toSet() }.orEmpty()
        val fresh = items.filter { item ->
            when (val value = unique?.let { item.getValue(it) }) {
                null -> never
                else -> never || (value !in added && value !in present)
            }
        }
        val changes = buildMap {
            if (fresh.isNotEmpty()) put(key, JsonArray(saved + fresh.map(::stamped)).toString())
            unique?.let { field ->
                val values = added + items.mapNotNull { it.getValue(field) }
                if (values != added) put(addedKey, JsonArray(values.map(::JsonPrimitive)).toString())
            }
        }
        if (changes.isEmpty()) return false
        store.edit(changes)
        if (fresh.isNotEmpty()) changeFlow.tryEmit(Change(appId, collection))
        return fresh.isNotEmpty()
    }

    private fun readTexts(key: String): Set<String> = store.get(key)?.let { value ->
        runCatching { (JsonText.parse(value) as JsonArray).mapNotNull { (it as? JsonPrimitive)?.content }.toSet() }.getOrNull()
    }.orEmpty()

    /**
     * Gives the object of [collection] whose "id" is [id] the values of [changes], without its [cleared] fields, and an
     * "updatedAt" of now (its "id" and "createdAt" stay as they are); null when there is no such object.
     */
    fun update(appId: String, collection: String, id: String, changes: JsonObject, cleared: Set<String> = emptySet()): JsonObject? {
        val items = items(appId, collection)
        val index = items.indexOfFirst { it.idText() == id }.takeIf { it >= 0 } ?: return null
        val updated = JsonObject(items[index] - (cleared - FIXED) + (changes - FIXED) + ("updatedAt" to JsonPrimitive(now())))
        save(appId, collection, items.toMutableList().apply { set(index, updated) })
        return updated
    }

    /** Removes the objects of [collection] that [matches] accepts, and says how many there were. */
    fun delete(appId: String, collection: String, matches: (JsonObject) -> Boolean): Int {
        val items = items(appId, collection)
        val kept = items.filterNot(matches)
        if (kept.size != items.size) save(appId, collection, kept)
        return items.size - kept.size
    }

    private fun save(appId: String, collection: String, items: List<JsonObject>) {
        store.put(key(appId, collection), JsonArray(items).toString())
        changeFlow.tryEmit(Change(appId, collection))
    }

    /** As a JSON array, so no app or collection name can make two keys meet. */
    private fun key(appId: String, collection: String, vararg more: String) =
        JsonArray((listOf(appId, collection) + more).map(::JsonPrimitive)).toString()

    private companion object {
        /** What identifies an object and when it was saved: an update never changes them. */
        val FIXED = setOf("id", "createdAt")

        /** Beside a collection: the unique values of the initial items already added to it, so none comes back. */
        const val ADDED = "initialItemsAdded"

        fun JsonObject.idText(): String? = (this["id"] as? JsonPrimitive)?.content

        @OptIn(ExperimentalUuidApi::class)
        fun randomId(): String = Uuid.random().toString()
    }
}
