package it.sapienza.appinterpreter.runtime

import android.content.Context
import it.sapienza.androidratio.appratio.R

/** The runtime's messages from the app's string resources. */
class AndroidRuntimeStrings(private val context: Context) : RuntimeStrings {
    override fun viewMissing(id: String) = context.getString(R.string.view_missing, id)
    override fun configurationInvalid(detail: String) = context.getString(R.string.configuration_invalid, detail)
    override val errorUnrecognized: String get() = context.getString(R.string.error_unrecognized)
    override fun screenDataUnrestorable(detail: String) = context.getString(R.string.screen_data_unrestorable, detail)
    override fun valueMissing(path: String) = context.getString(R.string.value_missing, path)
    override fun linkNotAllowed(url: String) = context.getString(R.string.link_not_allowed, url)
}
