package it.sapienza.appinterpreter.model

import kotlinx.serialization.json.Json

/**
 * How configurations become the model: views, form fields and events are chosen by their "type", unknown properties
 * are an error (as they were with Jackson), and nulls and defaults are left out when a model is written back.
 */
val ModelJson: Json = Json {
    explicitNulls = false
}
