package it.sapienza.appinterpreter.runtime.usecase

import it.sapienza.appinterpreter.json.getValue
import it.sapienza.appinterpreter.json.valueAt
import it.sapienza.appinterpreter.json.matchReplace
import it.sapienza.appinterpreter.model.view_model.FormView
import it.sapienza.appinterpreter.model.view_model.form_model.CheckBox
import it.sapienza.appinterpreter.model.view_model.form_model.helpers.FormElementImpl
import it.sapienza.appinterpreter.model.view_model.form_model.HiddenInput
import it.sapienza.appinterpreter.model.view_model.form_model.PhotosInput
import it.sapienza.appinterpreter.model.view_model.form_model.TagsInput
import it.sapienza.appinterpreter.model.view_model.form_model.Selector
import it.sapienza.appinterpreter.model.view_model.form_model.TimeInput
import it.sapienza.appinterpreter.utils.FormDates
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObject

/**
 * A form's starting values: the screen's data where it fits each field, else the field's default. A checkbox takes
 * true or false; a selector one of its options' values; a time HH:mm; a date is shown and submitted as written, even
 * if not dd/MM/yyyy. A hidden field takes its value, with placeholders filled in from the data. A form that adds a new
 * object ([addsNew]) shows only its defaults: a "New idea" screen opened on an app mustn't start with the app's own
 * description. Its hidden fields still take the screen's data, such as the app's id.
 */
class InitialFormValuesUseCase {
    operator fun invoke(form: FormView, data: JsonObject?, addsNew: Boolean = false): Map<String, Any?> =
        form.formElements.associate { item -> item.mapping to startingValue(item, data.takeUnless { addsNew && item !is HiddenInput }) }

    private fun startingValue(item: FormElementImpl, data: JsonObject?): Any? {
        val given = data?.getValue(item.mapping)?.takeUnless { it == "null" }
        return when (item) {
            is CheckBox -> given?.toBooleanStrictOrNull() ?: item.defaultValue()
            is Selector -> given?.takeIf { it in item.values } ?: item.defaultValue()
            is TimeInput -> given?.takeIf { FormDates.parseTime(it) != null } ?: item.defaultValue()
            // A hidden field's value is written in the form, filled in from the screen's data; else the data's own.
            is HiddenInput -> item.value?.matchReplace(data)?.takeIf { it.isNotEmpty() } ?: given
            // Photos are a list, kept as JSON.
            is PhotosInput -> data?.valueAt(item.mapping) as? JsonArray
            // Tags are a list too; text with commas (as older data might have) becomes one.
            is TagsInput -> data?.let { LoadStoredItemsUseCase.tagsOf(it, item.mapping) }?.takeIf { it.isNotEmpty() }
                ?.let { tags -> JsonArray(tags.map(::JsonPrimitive)) }
            else -> given ?: item.defaultValue()
        }
    }
}
