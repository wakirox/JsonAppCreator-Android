package it.sapienza.appinterpreter.runtime.usecase

import it.sapienza.appinterpreter.json.getValue
import it.sapienza.appinterpreter.model.view_model.StoredSort
import kotlinx.serialization.json.JsonObject

/**
 * [items] in [sorts]' order (see StoredSort): by the first, then by the next among those it leaves tied, and so on.
 * Objects without a field come last for that sort, and the sort is stable, so full ties keep the order they were
 * saved in.
 */
fun sorted(items: List<JsonObject>, sorts: List<StoredSort>?): List<JsonObject> {
    if (sorts.isNullOrEmpty()) return items
    return items.sortedWith(sorts.map(::comparator).reduce { first, next -> first.then(next) })
}

private fun comparator(sort: StoredSort): Comparator<JsonObject> {
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
    return compareBy(missingLast) { it.getValue(sort.by) }
}
