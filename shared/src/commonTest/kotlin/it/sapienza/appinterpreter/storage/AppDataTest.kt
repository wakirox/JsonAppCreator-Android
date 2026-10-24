package it.sapienza.appinterpreter.storage

import it.sapienza.appinterpreter.json.getValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AppDataTest {
    private val store = MemoryAppStore()
    private var ids = 0
    private val data = AppData(store, now = { 1_000L }, newId = { "id${++ids}" })

    private fun named(name: String) = JsonObject(mapOf("name" to JsonPrimitive(name)))

    @Test
    fun savedObjectsGetAnIdAndATimeAndKeepTheirOrder() {
        val first = data.add("app:ideas", "apps", named("Rainy"))
        data.add("app:ideas", "apps", named("Tasse"))

        assertEquals("id1", first.getValue("id"))
        assertEquals("1000", first.getValue("createdAt"))
        assertEquals(listOf("Rainy", "Tasse"), data.items("app:ideas", "apps").map { it.getValue("name") })
    }

    @Test
    fun anIdTheObjectAlreadyHasIsKept() {
        val saved = data.add("app:ideas", "apps", JsonObject(mapOf("id" to JsonPrimitive("mine"))))
        assertEquals("mine", saved.getValue("id"))
    }

    @Test
    fun eachAppAndCollectionIsSeparateEvenWithSlashesInTheirNames() {
        data.add("app:a/b", "c", named("one"))
        data.add("app:a", "b/c", named("two"))

        assertEquals(listOf("one"), data.items("app:a/b", "c").map { it.getValue("name") })
        assertEquals(listOf("two"), data.items("app:a", "b/c").map { it.getValue("name") })
        assertEquals(emptyList(), data.items("app:other", "c"))
    }

    @Test
    fun whatWasSavedIsThereForTheNextSession() {
        data.add("app:ideas", "apps", named("Rainy"))
        assertEquals(listOf("Rainy"), AppData(store).items("app:ideas", "apps").map { it.getValue("name") })
    }

    @Test
    fun unreadableDataReadsAsEmpty() {
        store.put("""["app:ideas","apps"]""", "not json")
        assertEquals(emptyList(), data.items("app:ideas", "apps"))
    }

    @Test
    fun realIdsDiffer() {
        val real = AppData(MemoryAppStore())
        assertNotEquals(real.add("a", "b", named("x")).getValue("id"), real.add("a", "b", named("y")).getValue("id"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun everySaveIsAnnounced() = runTest {
        val changes = mutableListOf<AppData.Change>()
        val listening = launch(UnconfinedTestDispatcher(testScheduler)) { data.changes.toList(changes) }

        data.add("app:ideas", "ideas", named("Dark mode"))

        assertEquals(listOf(AppData.Change("app:ideas", "ideas")), changes)
        listening.cancel()
    }
}
