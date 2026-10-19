package it.sapienza.appinterpreter.utils

import android.content.Context
import it.sapienza.androidratio.appratio.R

/** The current app on Android: [CurrentApp] on the "DomainController" preferences, with the raw-resource examples. */
object DomainController {
    /** The example shown on first launch, and when the active configuration can't be used. */
    val DEFAULT_CONFIGURATION = R.raw.v3_masterpieces

    fun jsonFile(context: Context): String = current(context).json()

    fun configurationId(context: Context): String = current(context).configurationId()

    /**
     * Makes [value] the current app and saves it in Your apps on the home page. [configurationId] keeps an app's
     * identity, and with it its saved or bundled API keys (for an app reopened from Your apps, or run unchanged); by
     * default it comes from the JSON itself.
     */
    fun setJsonFile(value: String, context: Context, configurationId: String? = null) {
        if (configurationId == null) current(context).set(value) else current(context).set(value, configurationId)
    }

    fun setJsonFile(resource: Int, context: Context) = current(context).set(bundled(resource, context))

    private fun current(context: Context) = CurrentApp(
        SharedPreferencesStore(context, PREFERENCES_NAME),
        AppLibrary.of(context)
    ) { bundled(DEFAULT_CONFIGURATION, context) }

    private fun bundled(resource: Int, context: Context) = BundledConfiguration(
        context.resources.getResourceEntryName(resource),
        context.resources.openRawResource(resource).bufferedReader().use { it.readText() }
    )

    private const val PREFERENCES_NAME = "DomainController"
}
