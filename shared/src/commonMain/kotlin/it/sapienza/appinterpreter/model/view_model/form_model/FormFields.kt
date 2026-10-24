package it.sapienza.appinterpreter.model.view_model.form_model

import it.sapienza.appinterpreter.model.view_model.form_model.helpers.FormElementImpl
import it.sapienza.appinterpreter.model.view_model.form_model.helpers.TextType
import it.sapienza.appinterpreter.utils.FormDates
import kotlinx.serialization.Serializable
import it.sapienza.appinterpreter.storage.Photos
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

@Serializable
class CheckBox(
    val value: Boolean = false,
    override val mandatory: Boolean? = null,
    override val title: String,
    override val description: String? = null,
    override val mapping: String
) : FormElementImpl() {
    override fun isSet(value: Any?): Boolean = !isRequired || value == true
    override fun defaultValue(): Any? = value
}

/** A date field. [min], [max] and [value] are instants in epoch milliseconds, read as Jackson read dates. */
@Serializable
class DateInput(
    @Serializable(with = JacksonDateSerializer::class) val min: Long? = null,
    @Serializable(with = JacksonDateSerializer::class) val max: Long? = null,
    @Serializable(with = JacksonDateSerializer::class) val value: Long? = null,
    override val mandatory: Boolean? = null,
    override val title: String,
    override val description: String? = null,
    override val mapping: String
) : FormElementImpl() {
    override fun isSet(value: Any?): Boolean = !isRequired || !(value as? String).isNullOrBlank()
    override fun defaultValue(): Any? = value?.let(FormDates::format)
}

/** A choice among [options]; [selection] and what it submits are an option's value. */
@Serializable
class Selector(
    val options: List<@Serializable(with = SelectorOptionSerializer::class) SelectorOption>,
    val selection: String? = null,
    override val mandatory: Boolean? = null,
    override val title: String,
    override val description: String? = null,
    override val mapping: String
) : FormElementImpl() {
    /** What the options submit. */
    val values: List<String> get() = options.map { it.value }

    override fun isSet(value: Any?): Boolean = !isRequired || (value as? String) in values
    override fun defaultValue(): Any? = selection?.takeIf { it in values }
}

/** An option of a selector: [label] is shown, [value] submitted. A plain string is both. */
@Serializable
class SelectorOption(val value: String, val label: String = value)

/** Reads an option written as "Painting" or as { "label": "Any type", "value": "" }, and writes it back the same way. */
object SelectorOptionSerializer : JsonTransformingSerializer<SelectorOption>(SelectorOption.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element is JsonPrimitive && element.isString) JsonObject(mapOf("value" to element)) else element

    override fun transformSerialize(element: JsonElement): JsonElement {
        val option = element as? JsonObject ?: return element
        val value = option["value"]
        return if (value != null && (option["label"] == null || option["label"] == value)) value else element
    }
}

/** A time of day, HH:mm, as a calendar event's time takes it. */
@Serializable
class TimeInput(
    val value: String? = null,
    override val mandatory: Boolean? = null,
    override val title: String,
    override val description: String? = null,
    override val mapping: String
) : FormElementImpl() {
    override fun isSet(value: Any?): Boolean = !isRequired || (value as? String)?.let(FormDates::parseTime) != null
    override fun defaultValue(): Any? = value?.takeIf { FormDates.parseTime(it) != null }
}

/**
 * A value the form sends without showing it, such as the id of the item on screen: [value], with placeholders
 * filled in from the screen's data.
 */
@Serializable
class HiddenInput(
    val value: String? = null,
    override val mapping: String,
    override val title: String = "",
    override val description: String? = null
) : FormElementImpl() {
    override val mandatory: Boolean? get() = null
    override fun isSet(value: Any?): Boolean = true
    override fun defaultValue(): Any? = null
}

/**
 * Photos picked from the phone's gallery, up to [max]: kept on the phone, and submitted as a list of references
 * (see Photos) that an image view shows.
 */
@Serializable
class PhotosInput(
    val max: Int? = null,
    override val mandatory: Boolean? = null,
    override val title: String,
    override val description: String? = null,
    override val mapping: String
) : FormElementImpl() {
    /** How many photos the field takes: [max], 10 by default. */
    val limit: Int get() = (max ?: Photos.DEFAULT_MAX).coerceAtLeast(1)

    override fun isSet(value: Any?): Boolean = !isRequired || !(value as? JsonArray).isNullOrEmpty()
    override fun defaultValue(): Any? = null
}

/** Short labels the user adds one by one (such as "watch", "ui"); submitted as a list of texts. */
@Serializable
class TagsInput(
    val placeholder: String? = null,
    override val mandatory: Boolean? = null,
    override val title: String,
    override val description: String? = null,
    override val mapping: String
) : FormElementImpl() {
    override fun isSet(value: Any?): Boolean = !isRequired || !(value as? JsonArray).isNullOrEmpty()
    override fun defaultValue(): Any? = null
}

@Serializable
class TextInput(
    val placeholder: String? = null,
    val value: String? = null,
    val textType: TextType? = TextType.plain,
    /** Takes several lines (Enter starts a new one), growing as it fills; for notes and descriptions. */
    val multiline: Boolean? = null,
    override val mandatory: Boolean? = null,
    override val title: String,
    override val description: String? = null,
    override val mapping: String
) : FormElementImpl() {
    override fun isSet(value: Any?): Boolean = !isRequired || !(value as? String).isNullOrBlank()
    override fun defaultValue(): Any? = value
}
