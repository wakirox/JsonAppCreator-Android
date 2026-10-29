package it.sapienza.appinterpreter.storage

import it.sapienza.appinterpreter.json.JsonText
import it.sapienza.appinterpreter.json.getValue
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * What the JSON apps keep on the phone: for each app (by its library id, see AppLibrary.idOf), named collections of
 * objects, in the order they were saved. Used from the main thread, as the screens' events run there.
 */
class AppData(
    private val store: AppStore,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val newId: () -> String = ::randomId
) {
    /** [collection] of the app [appId] changed. */
    data class Change(val appId: String, val collection: String)

    private val changeFlow = MutableSharedFlow<Change>(extraBufferCapacity = 16)
    val changes: SharedFlow<Change> = changeFlow.asSharedFlow()

    /** The saved objects of [collection], oldest first; none when nothing was saved (or what was can't be read). */
    fun items(appId: String, collection: String): List<JsonObject> = store.get(key(appId, collection))?.let { value ->
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
     * deleted since); says whether it saved anything. With [unique], a field that tells them apart, it also adds the
     * items whose value there it never added before and isn't saved: those a later version of the app brings. An item
     * the user deleted doesn't come back. A saved one takes a later version's values (a fixed photo, a new tip) for
     * every field the user left as the app first gave it; what the user changed (a tick, an edit) stays.
     */
    fun addFirst(appId: String, collection: String, items: List<JsonObject>, unique: String? = null): Boolean {
        val key = key(appId, collection)
        val never = !store.contains(key)
        val addedKey = key(appId, collection, ADDED)
        val seedsKey = key(appId, collection, SEEDS)
        val added = unique?.let { readTexts(addedKey) }.orEmpty()
        val saved = items(appId, collection)
        val present = unique?.let { field -> saved.mapNotNull { it.getValue(field) }.toSet() }.orEmpty()
        val fresh = items.filter { item ->
            when (val value = unique?.let { item.getValue(it) }) {
                null -> never
                else -> never || (value !in added && value !in present)
            }
        }
        val incoming = unique?.let { field ->
            items.mapNotNull { item -> item.getValue(field)?.let { it to item } }.toMap()
        }.orEmpty()
        val seeds = unique?.let { readObjects(seedsKey) }.orEmpty()
        val refreshed = saved.map { item ->
            val value = unique?.let { item.getValue(it) } ?: return@map item
            incoming[value]?.let { latest -> refreshed(item, seeds[value], latest) } ?: item
        }
        val changed = refreshed != saved
        val changes = buildMap {
            if (fresh.isNotEmpty() || changed) put(key, JsonArray(refreshed + fresh.map(::stamped)).toString())
            unique?.let { field ->
                val values = added + items.mapNotNull { it.getValue(field) }
                if (values != added) put(addedKey, JsonArray(values.map(::JsonPrimitive)).toString())
                if (incoming != seeds) put(seedsKey, JsonObject(incoming).toString())
            }
        }
        if (changes.isEmpty()) return false
        store.edit(changes)
        if (fresh.isNotEmpty() || changed) changeFlow.tryEmit(Change(appId, collection))
        return fresh.isNotEmpty() || changed
    }

    /**
     * [item], saved from [seed] (the app's earlier version of it), with [latest]'s values for the fields the user
     * didn't change, and without the ones the latest version dropped. Without a seed (saved before seeds were kept),
     * true/false values count as the user's (ticks) and the rest as the app's.
     */
    private fun refreshed(item: JsonObject, seed: JsonObject?, latest: JsonObject): JsonObject {
        fun usersOwn(field: String): Boolean = if (seed !=
            null
        ) {
            item[field] != seed[field]
        } else {
            (item[field] as? JsonPrimitive)?.let {
                !it.isString &&
                    it.content in BOOLEANS
            } ==
                true
        }
        val result = LinkedHashMap(item)
        latest.forEach { (field, value) -> if (field !in FIXED && !usersOwn(field)) result[field] = value }
        seed?.keys?.filter { it !in latest && it !in FIXED && !usersOwn(it) }?.forEach(result::remove)
        return JsonObject(result)
    }

    private fun readObjects(key: String): Map<String, JsonObject> = store.get(key)?.let { value ->
        runCatching {
            (JsonText.parse(value) as JsonObject).mapNotNull { (k, v) -> (v as? JsonObject)?.let { k to it } }.toMap()
        }.getOrNull()
    }.orEmpty()

    private fun readTexts(key: String): Set<String> = store.get(key)?.let { value ->
        runCatching {
            (JsonText.parse(value) as JsonArray).mapNotNull { (it as? JsonPrimitive)?.content }.toSet()
        }.getOrNull()
    }.orEmpty()

    /**
     * Gives the object of [collection] whose "id" is [id] the values of [changes], without its [cleared] fields, and an
     * "updatedAt" of now (its "id" and "createdAt" stay as they are); null when there is no such object.
     */
    fun update(
        appId: String,
        collection: String,
        id: String,
        changes: JsonObject,
        cleared: Set<String> = emptySet()
    ): JsonObject? {
        val items = items(appId, collection)
        val index = items.indexOfFirst { it.idText() == id }.takeIf { it >= 0 } ?: return null
        val updated =
            JsonObject(items[index] - (cleared - FIXED) + (changes - FIXED) + ("updatedAt" to JsonPrimitive(now())))
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

        /** Beside a collection: the initial items as the app last gave them, by unique value, to tell the user's changes. */
        const val SEEDS = "initialItemsSeeds"

        val BOOLEANS = setOf("true", "false")

        fun JsonObject.idText(): String? = (this["id"] as? JsonPrimitive)?.content

        @OptIn(ExperimentalUuidApi::class)
        fun randomId(): String = Uuid.random().toString()
    }
}
