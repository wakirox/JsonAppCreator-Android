package it.sapienza.androidratio.appratio

import it.sapienza.appinterpreter.extensions.getArray
import it.sapienza.appinterpreter.extensions.getObject
import it.sapienza.appinterpreter.extensions.getValue
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Paths in mappings and placeholders, including array items by index ("meals.0" or "meals[0]"). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JsonPathTest {
    private val response = JSONObject(
        """{ "meals": [ { "strMeal": "Arroz", "tags": ["rice", "seafood"] }, { "strMeal": "Paella" } ],
             "owner": { "name": "Ana" }, "nothing": null }"""
    )

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
        assertEquals("Arroz", response.getObject("meals.0")?.getString("strMeal"))
        assertEquals(2, response.getArray("meals")?.length())
    }
}
