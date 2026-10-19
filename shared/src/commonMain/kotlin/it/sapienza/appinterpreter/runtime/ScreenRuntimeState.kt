package it.sapienza.appinterpreter.runtime

import it.sapienza.appinterpreter.model.view_model.helper.MView
import kotlinx.serialization.json.JsonObject

/**
 * One rendered instance of a configured view.
 *
 * [definition] belongs to the parsed application. The payload is an immutable JSON object, so navigation data
 * cannot leak into another instance of the same definition.
 */
class ScreenRuntimeState private constructor(
    val definition: MView,
    val navigatedViewId: String?,
    /** The data the screen was opened with, if any. */
    val navigationPayload: JsonObject?
) {
    val data: JsonObject? = navigationPayload ?: definition.data

    companion object {
        fun create(
            definition: MView,
            payload: JsonObject?,
            navigatedViewId: String? = definition.id
        ): ScreenRuntimeState = ScreenRuntimeState(definition, navigatedViewId, payload)
    }
}
