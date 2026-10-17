package it.sapienza.appinterpreter.runtime.usecase

import it.sapienza.appinterpreter.extensions.getValue
import it.sapienza.appinterpreter.model.view_model.FormView
import it.sapienza.appinterpreter.model.view_model.form_model.CheckBox
import it.sapienza.appinterpreter.model.view_model.form_model.Selector
import org.json.JSONObject

/**
 * A form's starting values: the screen's data where it fits each field, else the field's default. A checkbox takes
 * true or false; a selector one of its options; a date is shown and submitted as written, even if not dd/MM/yyyy.
 */
class InitialFormValuesUseCase {
    operator fun invoke(form: FormView, data: JSONObject?): Map<String, Any?> = form.formElements.associate { item ->
        val given = data?.getValue(item.mapping)?.takeUnless { it == "null" }
        item.mapping to when (item) {
            is CheckBox -> given?.toBooleanStrictOrNull() ?: item.defaultValue()
            is Selector -> given?.takeIf { it in item.options } ?: item.defaultValue()
            else -> given ?: item.defaultValue()
        }
    }
}
