package it.sapienza.appinterpreter.runtime.usecase

import it.sapienza.appinterpreter.json.getValue
import it.sapienza.appinterpreter.model.view_model.StoredSort
import kotlinx.serialization.json.JsonObject

/**
 * [items] in [sort]'s order (see StoredSort). Objects without the field come last either way, and the sort is stable,
 * so ties keep the order they were saved in.
 */
fun sorted(items: List<JsonObject>, sort: StoredSort?): List<JsonObject> {
    sort ?: return items
    val listed = sort.order.orEmpty()
    val byValue = compareBy<String> { value -> listed.indexOf(value).takeIf { it >= 0 } ?: listed.size }
        .thenBy { it.toDoubleOrNull() == null }
        .thenBy { it.toDoubleOrNull() }
        .thenBy { it.lowercase() }
    val direction = if (sort.descending) byValue.reversed() else byValue
    val missingLast = Comparator<String?> { a, b ->
        when {
            a == null || b == null -> (a == null).compareTo(b == null)
            else -> direction.compare(a, b)
        }
    }
    return items.sortedWith(compareBy(missingLast) { it.getValue(sort.by) })
}
