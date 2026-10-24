package it.sapienza.appinterpreter.runtime.usecase

import it.sapienza.appinterpreter.json.getValue
import it.sapienza.appinterpreter.json.valueAt
import it.sapienza.appinterpreter.json.matchReplace
import it.sapienza.appinterpreter.model.view_model.FormView
import it.sapienza.appinterpreter.model.view_model.form_model.CheckBox
import it.sapienza.appinterpreter.model.view_model.form_model.HiddenInput
import it.sapienza.appinterpreter.model.view_model.form_model.PhotosInput
import it.sapienza.appinterpreter.model.view_model.form_model.Selector
import it.sapienza.appinterpreter.model.view_model.form_model.TimeInput
import it.sapienza.appinterpreter.utils.FormDates
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * A form's starting values: the screen's data where it fits each field, else the field's default. A checkbox takes
 * true or false; a selector one of its options' values; a time HH:mm; a date is shown and submitted as written, even
 * if not dd/MM/yyyy. A hidden field takes its value, with placeholders filled in from the data.
 */
class InitialFormValuesUseCase {
    operator fun invoke(form: FormView, data: JsonObject?): Map<String, Any?> = form.formElements.associate { item ->
        val given = data?.getValue(item.mapping)?.takeUnless { it == "null" }
        item.mapping to when (item) {
            is CheckBox -> given?.toBooleanStrictOrNull() ?: item.defaultValue()
            is Selector -> given?.takeIf { it in item.values } ?: item.defaultValue()
            is TimeInput -> given?.takeIf { FormDates.parseTime(it) != null } ?: item.defaultValue()
            // A hidden field's value is written in the form, filled in from the screen's data; else the data's own.
            is HiddenInput -> item.value?.matchReplace(data)?.takeIf { it.isNotEmpty() } ?: given
            // Photos are a list, kept as JSON.
            is PhotosInput -> data?.valueAt(item.mapping) as? JsonArray
            else -> given ?: item.defaultValue()
        }
    }
}
