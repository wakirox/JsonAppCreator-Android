package it.sapienza.appinterpreter.model.view_model

import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.event.RESTService
import it.sapienza.appinterpreter.model.view_model.helper.MView
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer

@Serializable
class ListView(
    val itemView: MView,
    val initData: RESTService? = null,
    val stored: StoredData? = null,
    /** Runs when an item is swiped towards the start (left), with that item as data: a delete, say. */
    val swipeToStart: Action? = null,
    /** The icon shown under an item swiped towards the start; a bin by default. */
    val swipeToStartIcon: ButtonIcon? = null,
    /** Runs when an item is swiped towards the end (right), with that item as data: marking it done, say. */
    val swipeToEnd: Action? = null,
    /** The icon shown under an item swiped towards the end; a tick by default. */
    val swipeToEndIcon: ButtonIcon? = null,
    val orientation: LayoutOrientation = LayoutOrientation.vertical,
    override val id: String? = null,
    override val action: Action? = null,
    override val mapping: String? = null,
    override val data: JsonObject? = null,
    override val visibleIf: String? = null,
    /** Items per row of a grid; 2 by default. */
    val columns: Int? = null,
    /** What an empty list says instead of "No data available"; may use placeholders. */
    val emptyText: String? = null
) : MView() {
    /** Items per row: 1 unless the list is a grid. */
    val columnCount: Int get() = if (orientation == LayoutOrientation.grid) (columns ?: 2).coerceIn(1, 6) else 1
}

/**
 * A list's items from what the app saved on the phone: the objects of [collection] whose fields equal [where]'s, whose
 * ${field} placeholders are filled from the list's data (such as the app a details screen shows).
 */
@Serializable
class StoredData(
    val collection: String,
    val where: JsonObject? = null,
    @Serializable(with = StoredSortsSerializer::class) val sort: List<StoredSort>? = null,
    /** Orders the user can choose above the list (the first is used until they do); they replace [sort]. */
    val sortOptions: List<SortOption>? = null,
    /** A field of tags (a list, or text with commas): the list offers a chip per tag in use, to show only those. */
    val filterBy: String? = null,
    /** Saved in [collection] the first time the list opens, when nothing was ever saved there: a checklist to tick off. */
    val initialItems: List<JsonObject>? = null,
    /** A field telling [initialItems] apart: those a later version adds are saved too (see AppData.addFirst). */
    val unique: String? = null
)

/** One order a stored list offers, by its [title] (keep it short: they share a row). */
@Serializable
class SortOption(val title: String, @Serializable(with = StoredSortsSerializer::class) val sort: List<StoredSort>)

/**
 * The order of a stored list, one StoredSort or a list of them: each later one orders the objects the earlier ones
 * leave tied, such as done ideas last, then by priority.
 */
object StoredSortsSerializer : JsonTransformingSerializer<List<StoredSort>>(ListSerializer(StoredSort.serializer())) {
    override fun transformDeserialize(element: JsonElement): JsonElement =
        element as? JsonArray ?: JsonArray(listOf(element))

    override fun transformSerialize(element: JsonElement): JsonElement =
        (element as? JsonArray)?.singleOrNull() ?: element
}

/**
 * The order of a stored list: by the field [by], with the values in [order] first and in that order (such as "High",
 * "Medium", "Low"), then the others, numbers by size and text alphabetically; [descending] turns it round. Objects that
 * tie keep the order they were saved in.
 */
@Serializable
class StoredSort(val by: String, val order: List<String>? = null, val descending: Boolean = false)
