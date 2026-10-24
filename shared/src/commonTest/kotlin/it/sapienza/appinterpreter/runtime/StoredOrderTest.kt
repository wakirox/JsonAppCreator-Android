package it.sapienza.appinterpreter.runtime

import it.sapienza.appinterpreter.json.getValue
import it.sapienza.appinterpreter.model.view_model.StoredSort
import it.sapienza.appinterpreter.runtime.usecase.sorted
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class StoredOrderTest {
    private fun item(title: String, priority: Any?) =
        JsonObject(listOfNotNull("title" to JsonPrimitive(title), priority?.let { "priority" to if (it is Int) JsonPrimitive(it) else JsonPrimitive(it.toString()) }).toMap())

    private fun titles(items: List<JsonObject>) = items.map { it.getValue("title") }

    private val ideas = listOf(item("a", "Low"), item("b", "High"), item("c", null), item("d", "Medium"), item("e", "High"), item("f", "Someday"))

    @Test
    fun listedValuesComeFirstInTheirOrderThenTheOthersThenTheMissing() {
        assertEquals(listOf("b", "e", "d", "a", "f", "c"), titles(sorted(ideas, StoredSort("priority", listOf("High", "Medium", "Low")))))
    }

    @Test
    fun descendingTurnsItRoundButKeepsTheMissingLastAndTiesInSavedOrder() {
        assertEquals(listOf("f", "a", "d", "b", "e", "c"), titles(sorted(ideas, StoredSort("priority", listOf("High", "Medium", "Low"), descending = true))))
    }

    @Test
    fun withoutAnOrderNumbersGoBySizeBeforeTextAlphabetically() {
        val items = listOf(item("a", 10), item("b", "banana"), item("c", 9), item("d", "Apple"), item("e", 9.5))
        assertEquals(listOf("c", "e", "a", "d", "b"), titles(sorted(items, StoredSort("priority"))))
    }

    @Test
    fun noSortKeepsTheSavedOrder() {
        assertEquals(titles(ideas), titles(sorted(ideas, null)))
    }
}
