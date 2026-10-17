package it.sapienza.appinterpreter.runtime.usecase

import it.sapienza.appinterpreter.model.view_model.FormView
import org.json.JSONObject

/** Checks a form's values: either the required fields still empty, or the JSON its event receives. */
class SubmitFormUseCase {

    sealed interface Result {
        class Missing(val message: String) : Result
        class Ready(val json: JSONObject) : Result
    }

    operator fun invoke(form: FormView, values: Map<String, Any?>): Result {
        val missing = form.formElements.filterNot { it.isSet(values[it.mapping]) }
        if (missing.isNotEmpty()) return Result.Missing("Please complete: " + missing.joinToString { it.title })
        // Dotted mappings ("a.b") become nested objects.
        val json = JSONObject()
        form.formElements.forEach { element ->
            val parts = element.mapping.split('.')
            var target = json
            parts.dropLast(1).forEach { key ->
                if (!target.has(key)) target.put(key, JSONObject())
                target = target.getJSONObject(key)
            }
            target.put(parts.last(), values[element.mapping])
        }
        return Result.Ready(json)
    }
}
