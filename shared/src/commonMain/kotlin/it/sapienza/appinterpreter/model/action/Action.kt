package it.sapienza.appinterpreter.model.action

import it.sapienza.appinterpreter.model.event.Event
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** What a tap (or long tap) does: an [event], or a reference to an action defined in the app's "actions". */
@Serializable
class Action(
    val id: String = "",
    val ref: String? = null,
    val data: JsonObject? = null,
    val type: ActionType = ActionType.click,
    val event: Event? = null
) {
    val referenceId: String
        get() = ref ?: id

    fun isReference() = ref != null || event == null

    /** Kept for callers compiled against the old model API. */
    fun isEmpty() = isReference()
}
