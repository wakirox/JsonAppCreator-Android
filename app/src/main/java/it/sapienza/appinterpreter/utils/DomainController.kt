package it.sapienza.appinterpreter.utils

import android.content.Context
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.security.ApiKeyPlaceholders

object DomainController {
    /** The example shown on first launch, and when the active configuration can't be used. */
    val DEFAULT_CONFIGURATION = R.raw.v3_masterpieces

    fun jsonFile(context: Context): String {
        val preferences = preferences(context)
        return preferences.getString(JSON_KEY, null) ?: read(DEFAULT_CONFIGURATION, context)
    }

    fun configurationId(context: Context): String {
        val preferences = preferences(context)
        return preferences.getString(CONFIGURATION_ID_KEY, null)
            ?: if (preferences.contains(JSON_KEY)) {
                ApiKeyPlaceholders.configurationId(jsonFile(context))
            } else {
                bundledId(context.resources.getResourceEntryName(DEFAULT_CONFIGURATION))
            }
    }

    fun setJsonFile(value: String, context: Context) {
        preferences(context).edit()
            .putString(JSON_KEY, value)
            .putString(CONFIGURATION_ID_KEY, ApiKeyPlaceholders.configurationId(value))
            .apply()
    }

    fun setJsonFile(resource: Int, context: Context) {
        val resourceName = context.resources.getResourceEntryName(resource)
        preferences(context).edit()
            .putString(JSON_KEY, read(resource, context))
            .putString(CONFIGURATION_ID_KEY, bundledId(resourceName))
            .apply()
    }

    private fun read(resource: Int, context: Context): String =
        context.resources.openRawResource(resource).bufferedReader().use { it.readText() }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private fun bundledId(resourceName: String) = "bundled:$resourceName"

    private const val PREFERENCES_NAME = "DomainController"
    private const val JSON_KEY = "main_file_sav"
    private const val CONFIGURATION_ID_KEY = "configuration_id"
}
