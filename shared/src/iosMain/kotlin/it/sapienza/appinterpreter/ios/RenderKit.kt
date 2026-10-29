package it.sapienza.appinterpreter.ios

import it.sapienza.appinterpreter.alerts.AlertsViewModel
import it.sapienza.appinterpreter.json.fillAddress
import it.sapienza.appinterpreter.json.getValue
import it.sapienza.appinterpreter.json.matchReplace
import it.sapienza.appinterpreter.json.optString
import it.sapienza.appinterpreter.json.valueAt
import it.sapienza.appinterpreter.markdown.Markdown
import it.sapienza.appinterpreter.model.Layout
import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.view_model.ButtonView
import it.sapienza.appinterpreter.model.view_model.CheckView
import it.sapienza.appinterpreter.model.view_model.FilesView
import it.sapienza.appinterpreter.model.view_model.FormView
import it.sapienza.appinterpreter.model.view_model.ImageView
import it.sapienza.appinterpreter.model.view_model.ListView
import it.sapienza.appinterpreter.model.view_model.TextView
import it.sapienza.appinterpreter.model.view_model.TextualView
import it.sapienza.appinterpreter.model.view_model.helper.MView
import it.sapienza.appinterpreter.runtime.ScreenViewModel
import it.sapienza.appinterpreter.runtime.ViewVisibility
import it.sapienza.appinterpreter.runtime.text
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** A one-off thing a screen asks the app to do, flattened for Swift (see [ScreenViewModel.Effect]). */
class ScreenEffect(
    /** openScreen, close, openUrl, copy, share, calendar, pdf or textPack. */
    val kind: String,
    val view: MView? = null,
    val payload: JsonObject? = null,
    val title: String? = null,
    val text: String? = null,
    val url: String? = null,
    val scheme: String? = null
)

/** The alert a screen shows. */
class AlertInfo(val text: String, val hasCancel: Boolean)

/** One styled run of Markdown text. */
class MarkdownRun(val text: String, val bold: Boolean, val italic: Boolean, val code: Boolean, val link: String?)

/** One block of Markdown: kind is heading, paragraph, item, quote or code. */
class MarkdownPart(
    val kind: String,
    val level: Int,
    val number: Int,
    val depth: Int,
    val runs: List<MarkdownRun>,
    val code: String
)

/** What the SwiftUI renderer needs from the model, in types Swift reads without ceremony. */
object RenderKit {
    /** The type of [view]: layout, text, button, image, list, form, check, files, or reference. */
    fun kind(view: MView): String = when (view) {
        is Layout -> "layout"
        is TextView -> "text"
        is ButtonView -> "button"
        is ImageView -> "image"
        is ListView -> "list"
        is FormView -> "form"
        is CheckView -> "check"
        is FilesView -> "files"
        else -> "reference"
    }

    /** True for a layout that lays its views out in a row. */
    fun isHorizontal(view: MView): Boolean = view is Layout && view.orientation == LayoutOrientation.horizontal

    fun isVerticalLayout(view: MView): Boolean = view is Layout && view.orientation != LayoutOrientation.horizontal

    fun isHorizontalList(view: ListView): Boolean = view.orientation == LayoutOrientation.horizontal

    /** [views] resolved, without those that aren't shown with [data]: they take no space, and no gap either. */
    fun shown(screen: ScreenViewModel, views: List<MView>, data: JsonObject?): List<MView> = views.mapNotNull { view ->
        screen.resolve(view).takeIf { ViewVisibility.isShown(it, it.data ?: data, reference = view) }
    }

    /** True when [model], reached with [parentData], is drawn. */
    fun isShown(screen: ScreenViewModel, model: MView, parentData: JsonObject?): Boolean {
        val resolved = screen.resolve(model)
        return ViewVisibility.isShown(resolved, resolved.data ?: parentData, reference = model)
    }

    fun text(view: TextualView, data: JsonObject?): String = view.text(data)

    fun label(view: TextualView, data: JsonObject?): String = view.label?.matchReplace(data).orEmpty()

    fun fill(text: String?, data: JsonObject?): String = text?.matchReplace(data).orEmpty()

    /** The text size a text or button view asks for, or -1. */
    fun fontSize(view: TextualView): Double = view.textSize?.toDouble() ?: -1.0

    /** The address of an image view's picture with [data]; null when it has none, or a placeholder lacks a value. */
    fun imageUrl(view: ImageView, data: JsonObject?): String? {
        val mapped = view.mapping?.let { data?.getValue(it) }
        return (mapped ?: view.url)?.fillAddress(data)?.takeIf { it.isNotBlank() }
    }

    /** The addresses of an image view that maps to a list (a photos field), else null. */
    fun imageUrls(view: ImageView, data: JsonObject?): List<String>? =
        (view.mapping?.let { data?.valueAt(it) } as? JsonArray)?.mapNotNull {
            (it as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)
        }

    fun imageWidth(view: ImageView): Int = view.size?.width ?: -1

    fun imageHeight(view: ImageView): Int = view.size?.height ?: -1

    fun textHeight(view: TextualView): Int = view.size?.height ?: -1

    /** The width a view asks for in its size (a layout's from its views'), or -1. */
    fun explicitWidth(view: MView): Int = width(view) ?: -1

    private fun width(view: MView): Int? = when (view) {
        is TextualView -> view.size?.width

        is ImageView -> view.size?.width

        is Layout -> when (view.orientation) {
            LayoutOrientation.horizontal ->
                view.views.mapNotNull { width(it) }.takeIf { it.size == view.views.size }?.sum()

            else -> view.views.mapNotNull { width(it) }.maxOrNull()
        }

        else -> null
    }

    /** A button that shows only its icon. */
    fun isIconOnly(view: MView): Boolean =
        view is ButtonView && view.icon != null && view.title.isNullOrBlank() && view.mapping == null

    /** The name of a button's icon (see ButtonIcon), or null. */
    fun iconName(view: ButtonView): String? = view.icon?.name

    fun isLongClick(screen: ScreenViewModel, view: MView): Boolean =
        screen.resolve(view.action)?.type?.name == "longclick"

    fun optString(data: JsonObject, key: String): String = data.optString(key)

    /** Markdown read into blocks to style. */
    fun markdown(text: String): List<MarkdownPart> = Markdown.parse(text).map { block ->
        when (block) {
            is Markdown.Block.Heading -> MarkdownPart("heading", block.level, 0, 0, runs(block.spans), "")

            is Markdown.Block.Paragraph -> MarkdownPart("paragraph", 0, 0, 0, runs(block.spans), "")

            is Markdown.Block.ListItem ->
                MarkdownPart("item", 0, block.number ?: 0, block.depth, runs(block.spans), "")

            is Markdown.Block.Quote -> MarkdownPart("quote", 0, 0, 0, runs(block.spans), "")

            is Markdown.Block.Code -> MarkdownPart("code", 0, 0, 0, emptyList(), block.text)
        }
    }

    private fun runs(spans: List<Markdown.Span>) = spans.map {
        MarkdownRun(it.text, it.bold, it.italic, it.code, it.link)
    }

    /** [screen]'s effects, flattened. */
    fun effects(screen: ScreenViewModel): Flow<ScreenEffect> = screen.effects.map { effect ->
        when (effect) {
            is ScreenViewModel.Effect.OpenScreen ->
                ScreenEffect("openScreen", view = effect.view, payload = effect.payload, title = effect.title)

            ScreenViewModel.Effect.Close -> ScreenEffect("close")

            is ScreenViewModel.Effect.OpenUrl -> ScreenEffect("openUrl", url = effect.url, scheme = effect.scheme)

            is ScreenViewModel.Effect.Copy -> ScreenEffect("copy", text = effect.text)

            is ScreenViewModel.Effect.Share -> ScreenEffect("share", text = effect.text)

            is ScreenViewModel.Effect.AddToCalendar -> ScreenEffect("calendar")

            is ScreenViewModel.Effect.Pdf -> ScreenEffect("pdf")

            is ScreenViewModel.Effect.TextPack -> ScreenEffect("textPack")
        }
    }

    /** The alert [alerts] shows, if any. */
    fun alert(alerts: AlertsViewModel): Flow<AlertInfo?> = alerts.alertState.map { alert ->
        alert?.let { AlertInfo(it.text, hasCancel = it.onCancel != null) }
    }
}
