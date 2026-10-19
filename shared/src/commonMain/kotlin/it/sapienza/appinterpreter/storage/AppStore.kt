package it.sapienza.appinterpreter.storage

/**
 * Small persistent text storage for one area of the app (Android: a SharedPreferences file; iOS would use a
 * UserDefaults suite). Writes are kept in memory at once and saved in the background, as SharedPreferences.apply().
 */
interface AppStore {
    fun get(key: String): String?
    fun contains(key: String): Boolean = get(key) != null
    fun keys(): Set<String>

    /** Writes every entry of [changes] together; a null value removes its key. */
    fun edit(changes: Map<String, String?>)

    fun put(key: String, value: String) = edit(mapOf(key to value))
    fun remove(key: String) = edit(mapOf(key to null))
}

/** An [AppStore] in memory only: for tests, and previews. */
class MemoryAppStore(initial: Map<String, String> = emptyMap()) : AppStore {
    private val values = LinkedHashMap(initial)
    override fun get(key: String) = values[key]
    override fun keys(): Set<String> = values.keys.toSet()
    override fun edit(changes: Map<String, String?>) {
        changes.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
    }
}
