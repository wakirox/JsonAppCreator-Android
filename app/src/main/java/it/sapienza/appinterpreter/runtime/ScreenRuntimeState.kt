package it.sapienza.appinterpreter.runtime

import it.sapienza.appinterpreter.model.view_model.helper.MView
import org.json.JSONObject

/**
 * One rendered instance of a configured view.
 *
 * [definition] belongs to the parsed application and must be treated as immutable. The payload is
 * copied when the screen is created so navigation data cannot leak into another instance of the
 * same definition.
 */
class ScreenRuntimeState private constructor(
    val definition: MView,
    val navigatedViewId: String?,
    private val payloadSnapshot: String?
) {
    val data: JSONObject? = payloadSnapshot?.let(::JSONObject) ?: definition.dataObj

    val navigationPayload: JSONObject?
        get() = payloadSnapshot?.let(::JSONObject)

    companion object {
        fun create(
            definition: MView,
            payload: JSONObject?,
            navigatedViewId: String? = definition.id
        ): ScreenRuntimeState = ScreenRuntimeState(
            definition = definition,
            navigatedViewId = navigatedViewId,
            payloadSnapshot = payload?.toString()
        )
    }
}
