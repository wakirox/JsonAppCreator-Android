package it.sapienza.appinterpreter.utils

import android.content.Context
import org.json.JSONObject

/**
 * The apps the user has made or opened, kept on the phone under their names so they can be reopened, pinned to the
 * home screen, or deleted. An app saved again under the same name replaces the earlier version.
 */
object AppLibrary {
    data class SavedApp(val id: String, val name: String, val json: String, val savedAt: Long)

    fun save(context: Context, json: String) {
        val name = runCatching { JSONObject(json).optString("name") }.getOrNull()?.takeIf { it.isNotBlank() } ?: return
        preferences(context).edit()
            .putString(idOf(name), JSONObject().put("name", name).put("json", json).put("savedAt", System.currentTimeMillis()).toString())
            .apply()
    }

    /** The saved apps, most recently saved first. */
    fun all(context: Context): List<SavedApp> = preferences(context).all.mapNotNull { (id, value) ->
        runCatching {
            val entry = JSONObject(value as String)
            SavedApp(id, entry.getString("name"), entry.getString("json"), entry.getLong("savedAt"))
        }.getOrNull()
    }.sortedByDescending { it.savedAt }

    fun get(context: Context, id: String): SavedApp? = all(context).firstOrNull { it.id == id }

    fun delete(context: Context, id: String) = preferences(context).edit().remove(id).apply()

    /** A stable id for an app name: the shortcut and the library entry share it. */
    fun idOf(name: String): String = "app:" + name.trim().lowercase()

    private fun preferences(context: Context) = context.getSharedPreferences("AppLibrary", Context.MODE_PRIVATE)
}
