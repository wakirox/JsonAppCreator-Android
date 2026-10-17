package it.sapienza.androidratio.appratio

import it.sapienza.appinterpreter.model.view_model.ListView
import it.sapienza.appinterpreter.runtime.usecase.FetchJsonUseCase
import it.sapienza.appinterpreter.runtime.usecase.LoadListItemsUseCase
import it.sapienza.appinterpreter.utils.AppParser
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LoadListItemsUseCaseTest {
    private val loadItems = LoadListItemsUseCase(FetchJsonUseCase())

    @Test
    fun aListWithoutARequestTakesItsItemsFromTheScreensData() = runBlocking {
        val list = AppParser(
            """
            {
              "name": "Lists", "version": "1.0", "schemaVersion": 3,
              "mainView": { "type": "list", "mapping": "books", "itemView": { "type": "text", "mapping": "title" } }
            }
            """
        ).parseApplication2().mainView as ListView

        val items = loadItems(list, JSONObject("""{ "books": [ { "title": "Middlemarch" }, { "title": "The Leopard" } ] }"""))

        assertEquals(listOf("Middlemarch", "The Leopard"), items.map { it.getString("title") })
    }

    @Test
    fun connectionProblemsReadPlainly() {
        assertEquals(
            "Unable to load data. Check your connection and try again.",
            loadItems.errorMessage("Unable to resolve host \"example.com\"")
        )
        assertEquals("Unable to load data. Request failed with 500", loadItems.errorMessage("Request failed with 500"))
    }
}
