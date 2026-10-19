package it.sapienza.appinterpreter.runtime

import it.sapienza.appinterpreter.model.ModelApplication
import it.sapienza.appinterpreter.validation.ConfigurationValidator
import it.sapienza.appinterpreter.validation.V3Schema
import it.sapienza.appinterpreter.validation.ValidatedConfigurationParser
import kotlin.concurrent.Volatile

/** The JSON of the app that runs now (Android: DomainController). */
fun interface CurrentConfiguration {
    fun json(): String
}

/** The current JSON app, parsed once and shared by its screens. */
class AppRepository(
    private val current: CurrentConfiguration,
    private val parser: ValidatedConfigurationParser = ValidatedConfigurationParser(ConfigurationValidator.fromSchema(V3Schema.JSON))
) {
    @Volatile
    private var parsed: Pair<String, ModelApplication>? = null

    /** The app [current] gives, parsed again only when its JSON changed. Throws when it doesn't parse. */
    fun current(): ModelApplication {
        val json = current.json()
        parsed?.let { (text, app) -> if (text == json) return app }
        // Two screens loading at once may both parse; either result is the same app.
        return parser.parse(json).also { parsed = json to it }
    }
}
