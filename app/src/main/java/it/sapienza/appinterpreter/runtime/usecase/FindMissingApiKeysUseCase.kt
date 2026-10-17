package it.sapienza.appinterpreter.runtime.usecase

import android.content.Context
import it.sapienza.appinterpreter.security.ConfigurationApiKeys
import it.sapienza.appinterpreter.utils.DomainController

/** The current app's API keys this phone lacks, or null when it has them all. */
class FindMissingApiKeysUseCase(private val context: Context) {

    class Missing(val json: String, val names: Set<String>, val savedNames: Set<String>)

    operator fun invoke(): Missing? {
        val json = DomainController.jsonFile(context)
        val names = ConfigurationApiKeys.placeholderNames(json)
        val saved = ConfigurationApiKeys.availableNamesForCurrentConfiguration(json)
        return if (names.all(saved::contains)) null else Missing(json, names, saved)
    }
}
