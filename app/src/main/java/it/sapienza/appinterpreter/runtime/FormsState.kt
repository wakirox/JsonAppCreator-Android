package it.sapienza.appinterpreter.runtime

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.SavedStateHandle
import it.sapienza.appinterpreter.model.view_model.FormView
import it.sapienza.appinterpreter.runtime.usecase.InitialFormValuesUseCase
import it.sapienza.appinterpreter.runtime.usecase.SubmitFormUseCase
import org.json.JSONObject

/**
 * The values and errors of a screen's forms. Values are also written to [saved], so a half-filled form survives
 * Android closing the app in the background. A form's definition is shared (by list rows, and by screens opened
 * again with other data), so it is only read, never written.
 */
class FormsState(
    private val saved: SavedStateHandle,
    private val initialValues: InitialFormValuesUseCase = InitialFormValuesUseCase(),
    private val submit: SubmitFormUseCase = SubmitFormUseCase(),
) {
    private val forms = mutableMapOf<String, SnapshotStateMap<String, Any?>>()
    private val errors = mutableStateMapOf<String, String>()

    /** [form]'s values when it shows [data]: the saved ones, else its starting values. */
    fun values(form: FormView, data: JSONObject?): Map<String, Any?> = state(form, data)

    fun set(form: FormView, data: JSONObject?, mapping: String, value: Any?) {
        val values = state(form, data)
        values[mapping] = value
        saved[key(form, data)] = JSONObject(values.toMap()).toString()
    }

    /** Why [form] wasn't sent, if it wasn't. */
    fun error(form: FormView, data: JSONObject?): String? = errors[key(form, data)]

    /** The JSON to send when every required field is filled in; else null, and [error] says which aren't. */
    fun submit(form: FormView, data: JSONObject?): JSONObject? {
        val key = key(form, data)
        return when (val result = submit(form, state(form, data))) {
            is SubmitFormUseCase.Result.Missing -> null.also { errors[key] = result.message }
            is SubmitFormUseCase.Result.Ready -> result.json.also { errors.remove(key) }
        }
    }

    private fun state(form: FormView, data: JSONObject?) = forms.getOrPut(key(form, data)) {
        val restored = saved.get<String>(key(form, data))?.let(::JSONObject)
        mutableStateMapOf<String, Any?>().apply {
            initialValues(form, data).forEach { (mapping, initial) ->
                val kept = restored?.takeIf { it.has(mapping) }?.opt(mapping)
                put(mapping, if (kept == null || kept == JSONObject.NULL) initial else kept)
            }
        }
    }

    /** Stable across recreation, so saved values find their form again; identical forms on identical data share it. */
    private fun key(form: FormView, data: JSONObject?) =
        "form:${form.id.orEmpty()}:${form.formElements.joinToString(",") { it.mapping }}:${data?.toString().hashCode()}"
}
