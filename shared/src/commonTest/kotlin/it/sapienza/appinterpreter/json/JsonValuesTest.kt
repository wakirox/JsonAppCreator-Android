package it.sapienza.appinterpreter.json

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        assertEquals("Ana likes Arroz, ", "${'$'}{owner.name} likes ${'$'}{meals.0.strMeal}, ${'$'}{unknown}".matchReplace(response))
        assertEquals("a", "a".matchReplace(JsonObject(emptyMap())))
    }

    @Test
    fun aPlaceholderWithoutAValueBecomesEmpty() {
        assertEquals("", "${'$'}{x}".matchReplace(null))
        assertEquals("By ", "By ${'$'}{owner.age}".matchReplace(response))
        assertEquals("[]", "[${'$'}{nothing}]".matchReplace(response))
        assertEquals("", "${'$'}{meals.5.strMeal}${'$'}{missing.name}".matchReplace(response))
        // A value with a dollar sign is inserted as it is.
        assertEquals("${'$'}1", "${'$'}{v}".matchReplace(JsonText.parse("""{ "v": "${'$'}1" }""")!!.jsonObject))
    }

    @Test
    fun missingPlaceholdersAreListedOnce() {
        assertEquals(listOf("nothing", "missing.id"), "${'$'}{owner.name}/${'$'}{nothing}/${'$'}{missing.id}/${'$'}{nothing}".missingPlaceholders(response))
        assertEquals(emptyList(), "https://x/${'$'}{meals.1.strMeal}".missingPlaceholders(response))
        assertEquals(listOf("id"), "${'$'}{id}".missingPlaceholders(null))
    }

    @Test
    fun anAddressIsFilledOnlyWhenEveryPlaceholderHasAValue() {
        assertEquals("https://x/Paella.jpg", "https://x/${'$'}{meals.1.strMeal}.jpg".fillAddress(response))
        assertNull("https://x/${'$'}{meals.2.strMeal}.jpg".fillAddress(response))
        assertNull("${'$'}{nothing}".fillAddress(response))
        assertEquals("https://x/a.jpg", "https://x/a.jpg".fillAddress(null))
    }

    @Test
    fun valuesInAnAddressArePercentEncoded() {
        val data = JsonText.parse(
            """{ "type": "arms & armor", "q": "a#b?c=d", "city": "São Paulo", "plus": "1+1", "slash": "a/b",
                 "image": "https://example.com/a b.jpg?w=640", "id": 42 }"""
        )!!.jsonObject
        assertEquals("https://x/?type=arms%20%26%20armor&limit=40", "https://x/?type=${'$'}{type}&limit=40".fillAddress(data))
        assertEquals("https://x/?q=a%23b%3Fc%3Dd", "https://x/?q=${'$'}{q}".fillAddress(data))
        assertEquals("https://x/?city=S%C3%A3o%20Paulo", "https://x/?city=${'$'}{city}".fillAddress(data))
        assertEquals("https://x/?n=1%2B1&p=a%2Fb", "https://x/?n=${'$'}{plus}&p=${'$'}{slash}".fillAddress(data))
        assertEquals("https://x/creators/42", "https://x/creators/${'$'}{id}".fillAddress(data))
        // The template is never touched: only what a placeholder inserts.
        assertEquals("https://x/?type=Arms%20and%20Armor&q=a%23b%3Fc%3Dd", "https://x/?type=Arms%20and%20Armor&q=${'$'}{q}".fillAddress(data))
    }

    @Test
    fun aPlaceholderThatIsTheWholeAddressIsLeftAlone() {
        val data = JsonText.parse("""{ "image": "https://example.com/a%20b.jpg?w=640", "q": "a b" }""")!!.jsonObject
        assertEquals("https://example.com/a%20b.jpg?w=640", "${'$'}{image}".fillAddress(data))
        assertEquals("https://example.com/a%20b.jpg?w=640&q=a%20b", "${'$'}{image}&q=${'$'}{q}".fillAddress(data))
        // Text that isn't an address still fills in as it is.
        assertEquals("Search a b", "Search ${'$'}{q}".matchReplace(data))
    }

    @Test
    fun presenceIsWhatVisibleIfTests() {
        val data = JsonText.parse(
            """{ "text": "a", "blank": " ", "empty": "", "yes": true, "no": false, "zero": 0, "zeroPoint": 0.0, "one": 1,
                 "list": [1], "noList": [], "object": { "a": 1 }, "noObject": {}, "nothing": null }"""
        )!!.jsonObject
        listOf("text", "yes", "one", "list", "object", "object.a", "list.0").forEach { assertTrue(data.hasValueAt(it), it) }
        listOf("blank", "empty", "no", "zero", "zeroPoint", "noList", "noObject", "nothing", "missing", "list.3").forEach {
            assertFalse(data.hasValueAt(it), it)
        }
        assertFalse((null as JsonObject?).hasValueAt("text"))
    }
}
