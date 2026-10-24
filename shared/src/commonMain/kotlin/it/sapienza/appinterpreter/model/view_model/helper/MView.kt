package it.sapienza.appinterpreter.model.view_model.helper

import it.sapienza.appinterpreter.model.Layout
import it.sapienza.appinterpreter.model.TypedSerializer
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.view_model.ButtonView
import it.sapienza.appinterpreter.model.view_model.CheckView
import it.sapienza.appinterpreter.model.view_model.FormView
import it.sapienza.appinterpreter.model.view_model.ImageView
import it.sapienza.appinterpreter.model.view_model.ListView
import it.sapienza.appinterpreter.model.view_model.TextView
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * A view of the app, chosen by its "type" (layout, text, button, image, list or form), or a [ViewReference] to a
 * reusable view when it has none.
 */
@Serializable(with = MViewSerializer::class)
abstract class MView {
    abstract val id: String?
    abstract val action: Action?
    abstract val mapping: String?
    abstract val data: JsonObject?

    /**
     * A path in the view's data: the view is shown only when something is there (see hasValueAt). "!path" shows it
     * only when nothing is. Null: always shown.
     */
    open val visibleIf: String? get() = null

    open val referenceId: String?
        get() = id

    open fun isReference(): Boolean = false

    /** Kept for callers compiled against the old model API. */
    open fun isEmpty(): Boolean = isReference()
}

/** A reference to a reusable view, by "ref" (preferred) or, in older configurations, by "id" alone. */
@Serializable
class ViewReference(
    override val id: String? = null,
    override val action: Action? = null,
    override val mapping: String? = null,
    override val data: JsonObject? = null,
    val ref: String? = null,
    override val visibleIf: String? = null
) : MView() {
    override val referenceId: String?
        get() = ref ?: id

    override fun isReference(): Boolean = true
}

/** A view with a "type" is that type's view; one without is a [ViewReference]. */
object MViewSerializer : TypedSerializer<MView>(
    "MView",
    listOf(
        Type("layout", Layout.serializer()) { it is Layout },
        Type("text", TextView.serializer()) { it is TextView },
        Type("button", ButtonView.serializer()) { it is ButtonView },
        Type("image", ImageView.serializer()) { it is ImageView },
        Type("list", ListView.serializer()) { it is ListView },
        Type("form", FormView.serializer()) { it is FormView },
        Type("check", CheckView.serializer()) { it is CheckView }
    ),
    untyped = ViewReference.serializer()
)
