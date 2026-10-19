package it.sapienza.appinterpreter.utils

import it.sapienza.appinterpreter.security.ApiKeyPlaceholders
import it.sapienza.appinterpreter.storage.AppStore

/** A configuration that comes with the app, by its resource name (such as "v3_masterpieces"). */
class BundledConfiguration(val name: String, val json: String) {
    val configurationId: String get() = "bundled:$name"
}

/**
 * The app that runs: its JSON and the identity its API keys are kept under. Without one, [defaultConfiguration] (the
 * example shown on first launch) runs.
 */
class CurrentApp(
    private val store: AppStore,
    private val library: AppLibrary,
    private val defaultConfiguration: () -> BundledConfiguration
) {
    fun json(): String = store.get(JSON_KEY) ?: defaultConfiguration().json

    fun configurationId(): String =
        store.get(CONFIGURATION_ID_KEY)
            ?: if (store.contains(JSON_KEY)) ApiKeyPlaceholders.configurationId(json()) else defaultConfiguration().configurationId

    /**
     * Makes [json] the current app and saves it in Your apps on the home page. [configurationId] keeps an app's
     * identity, and with it its saved or bundled API keys (for an app reopened from Your apps, or run unchanged); by
     * default it comes from the JSON itself.
     */
    fun set(json: String, configurationId: String = ApiKeyPlaceholders.configurationId(json)) {
        library.save(json, configurationId)
        store.edit(mapOf(JSON_KEY to json, CONFIGURATION_ID_KEY to configurationId))
    }

    /** An example: not saved in Your apps, since the home page lists the examples separately. */
    fun set(bundled: BundledConfiguration) {
        store.edit(mapOf(JSON_KEY to bundled.json, CONFIGURATION_ID_KEY to bundled.configurationId))
    }

    companion object {
        const val JSON_KEY = "main_file_sav"
        const val CONFIGURATION_ID_KEY = "configuration_id"
    }
}
