package it.sapienza.appinterpreter.json

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Paths in mappings and placeholders, including array items by index ("meals.0" or "meals[0]"). */
class JsonValuesTest {
    private val response = JsonText.parse(
        """{ "meals": [ { "strMeal": "Arroz", "tags": ["rice", "seafood"] }, { "strMeal": "Paella" } ],
             "owner": { "name": "Ana" }, "nothing": null, "price": 1.50, "count": 3 }"""
    )!!.jsonObject

    @Test
    fun plainAndNestedPaths() {
        assertEquals("Ana", response.getValue("owner.name"))
        assertNull(response.getValue("owner.age"))
        assertNull(response.getValue("missing.name"))
        assertNull(response.getValue("nothing"))
    }

    @Test
    fun arrayItemsByIndex() {
        assertEquals("Arroz", response.getValue("meals.0.strMeal"))
        assertEquals("Paella", response.getValue("meals[1].strMeal"))
        assertEquals("seafood", response.getValue("meals.0.tags.1"))
        assertEquals("rice, seafood", response.getValue("meals.0.tags"))
        assertNull(response.getValue("meals.5.strMeal"))
        assertEquals("Arroz", response.getObject("meals.0")?.get("strMeal")?.jsonPrimitive?.content)
        assertEquals(2, response.getArray("meals")?.size)
    }

    @Test
    fun numbersReadAsTheyAlwaysDid() {
        assertEquals("1.5", response.getValue("price"))
        assertEquals("3", response.getValue("count"))
        assertEquals("3", response.optString("count"))
        assertEquals("", response.optString("missing"))
    }

    @Test
    fun placeholdersAreFilledFromTheData() {
        assertEquals("Ana likes Arroz, ${'$'}{unknown}", "${'$'}{owner.name} likes ${'$'}{meals.0.strMeal}, ${'$'}{unknown}".matchReplace(response))
        assertEquals("${'$'}{x}", "${'$'}{x}".matchReplace(null))
        assertEquals("a", "a".matchReplace(JsonObject(emptyMap())))
    }
}
