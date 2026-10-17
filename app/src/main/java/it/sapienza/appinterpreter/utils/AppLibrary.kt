package it.sapienza.appinterpreter.utils

import android.content.Context
import org.json.JSONObject

/**
 * The apps the user has made or opened, kept on the phone under their names so they can be reopened, pinned to the
 * home screen, or deleted. An app saved again under the same name replaces the earlier version. The bundled examples
 * aren't saved here: they have their own list.
 */
object AppLibrary {
    /** [configurationId] is kept so an app reopened later finds its saved or bundled API keys again. */
    data class SavedApp(val id: String, val name: String, val json: String, val savedAt: Long, val configurationId: String) {
        /** The emoji the app chose for its icon, if any. */
        val icon: String? by lazy { iconOf(json) }
    }

    /** Saves [json] under its name; nothing happens for an app whose name is missing or blank. */
    fun save(context: Context, json: String, configurationId: String): SavedApp? {
        val name = nameOf(json) ?: return null
        val app = SavedApp(idOf(name), name, json, System.currentTimeMillis(), configurationId)
        val entry = JSONObject()
            .put("name", app.name).put("json", app.json).put("savedAt", app.savedAt).put("configurationId", configurationId)
        preferences(context).edit().putString(app.id, entry.toString()).apply()
        return app
    }

    /** The saved apps, most recently opened first. */
    fun all(context: Context): List<SavedApp> =
        preferences(context).all.keys.mapNotNull { get(context, it) }.sortedByDescending { it.savedAt }

    fun get(context: Context, id: String): SavedApp? = preferences(context).getString(id, null)?.let { value ->
        runCatching {
            val entry = JSONObject(value)
            SavedApp(id, entry.getString("name"), entry.getString("json"), entry.getLong("savedAt"), entry.optString("configurationId"))
        }.getOrNull()
    }

    fun delete(context: Context, id: String) = preferences(context).edit().remove(id).apply()

    /** "My app", or "My app 2", "My app 3"… when that name is taken, so a new app doesn't replace a saved one. */
    fun freeName(context: Context, name: String): String =
        generateSequence(1) { it + 1 }.map { if (it == 1) name else "$name $it" }.first { get(context, idOf(it)) == null }

    /** A stable id for an app name: the shortcut and the library entry share it. Names differing only in case match. */
    fun idOf(name: String): String = "app:" + name.trim().lowercase()

    fun iconOf(json: String): String? = runCatching { JSONObject(json).optString("icon") }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }

    fun nameOf(json: String): String? = runCatching { JSONObject(json).optString("name") }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }

    private fun preferences(context: Context) = context.getSharedPreferences("AppLibrary", Context.MODE_PRIVATE)
}
