package it.sapienza.appinterpreter.runtime

import it.sapienza.appinterpreter.validation.fromAssets
import it.sapienza.appinterpreter.validation.fromSchema
import android.content.Context
import it.sapienza.appinterpreter.model.ModelApplication
import it.sapienza.appinterpreter.utils.DomainController
import it.sapienza.appinterpreter.validation.ConfigurationValidator
import it.sapienza.appinterpreter.validation.ValidatedConfigurationParser

/** The current JSON app, parsed once and shared by its screens. */
object AppRepository {
    private var parsed: Pair<String, ModelApplication>? = null

    /** The app [DomainController] holds, parsed again only when its JSON changed. Throws when it doesn't parse. */
    @Synchronized
    fun current(context: Context): ModelApplication {
        val json = DomainController.jsonFile(context)
        parsed?.let { (text, app) -> if (text == json) return app }
        return ValidatedConfigurationParser(ConfigurationValidator.fromAssets(context)).parse(json)
            .also { parsed = json to it }
    }
}
