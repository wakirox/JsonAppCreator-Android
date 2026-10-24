package it.sapienza.appinterpreter.model.view_model

import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.view_model.helper.MView
import it.sapienza.appinterpreter.model.view_model.helper.Size
import it.sapienza.appinterpreter.model.view_model.helper.TextStyle
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** A view showing text: a [TextView], or a [ButtonView] (which was a TextView subclass before kotlinx.serialization). */
abstract class TextualView : MView() {
    abstract val textSize: Float?
    abstract val size: Size?
    abstract val textStyle: TextStyle?
    abstract val title: String?
    abstract val label: String?

    /** The label's style, when it differs from the text's; null: the label looks like the text. */
    open val labelStyle: TextStyle? get() = null

    /** How the text is written: plain (null) or simple HTML. */
    open val format: TextFormat? get() = null
}

@Serializable
enum class TextFormat {
    plain,
    /** Simple HTML: <b>, <i>, <em>, <strong>, <u>, <br>, <p> and <a href>. */
    html
}

@Serializable
class TextView(
    override val textSize: Float? = null,
    override val size: Size? = null,
    override val textStyle: TextStyle? = TextStyle.regular,
    override val title: String? = null,
    override val label: String? = null,
    override val id: String? = null,
    override val action: Action? = null,
    override val mapping: String? = null,
    override val data: JsonObject? = null,
    override val visibleIf: String? = null,
    /** Draw the label even when the text it labels is empty, as before visibleIf existed. */
    val showWhenEmpty: Boolean? = null,
    override val labelStyle: TextStyle? = null,
    override val format: TextFormat? = null
) : TextualView()

@Serializable
class ButtonView(
    override val textSize: Float? = null,
    override val size: Size? = null,
    override val textStyle: TextStyle? = TextStyle.regular,
    override val title: String? = null,
    override val label: String? = null,
    override val id: String? = null,
    override val action: Action? = null,
    override val mapping: String? = null,
    override val data: JsonObject? = null,
    override val visibleIf: String? = null
) : TextualView()
