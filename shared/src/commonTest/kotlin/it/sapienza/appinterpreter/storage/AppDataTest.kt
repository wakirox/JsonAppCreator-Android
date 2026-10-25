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
import kotlin.test.assertNull

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
    fun firstItemsAreSavedOnlyInACollectionNothingWasEverSavedIn() {
        assertEquals(true, data.addFirst("app:trip", "places", listOf(named("Parliament"), named("Bastion"))))
        assertEquals(listOf("id1", "id2"), data.items("app:trip", "places").map { it.getValue("id") })
        assertEquals(false, data.addFirst("app:trip", "places", listOf(named("Baths"))))

        data.delete("app:trip", "places") { true }
        assertEquals(false, data.addFirst("app:trip", "places", listOf(named("Baths"))), "not after the user emptied it")
        assertEquals(emptyList(), data.items("app:trip", "places"))
        assertEquals(false, data.addFirst("app:trip", "other", emptyList()))
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

    @Test
    fun anUpdateChangesValuesButNeverTheIdOrTime() {
        val first = data.add("app:ideas", "ideas", named("Dark mode"))
        data.add("app:ideas", "ideas", named("Widgets"))
        val changes = JsonObject(mapOf("done" to JsonPrimitive(true), "id" to JsonPrimitive("other"), "createdAt" to JsonPrimitive(5)))

        val updated = data.update("app:ideas", "ideas", "id1", changes)

        assertEquals(listOf("id1", "1000", "Dark mode", "true"), listOf("id", "createdAt", "name", "done").map { updated?.getValue(it) })
        assertEquals("1000", updated?.getValue("updatedAt"), "a change records when it happened")
        assertEquals(null, data.items("app:ideas", "ideas")[1].getValue("updatedAt"), "an unchanged object has none")
        assertEquals(listOf("true", null), data.items("app:ideas", "ideas").map { it.getValue("done") })
        assertEquals(first.getValue("id"), data.items("app:ideas", "ideas")[0].getValue("id"))
    }

    @Test
    fun updatingWhatIsNotSavedChangesNothing() {
        data.add("app:ideas", "ideas", named("Dark mode"))
        assertNull(data.update("app:ideas", "ideas", "nope", named("x")))
        assertEquals(listOf("Dark mode"), data.items("app:ideas", "ideas").map { it.getValue("name") })
    }

    @Test
    fun deleteRemovesWhatMatchesAndCountsIt() {
        listOf("a", "b", "c").forEach { data.add("app:ideas", "ideas", named(it)) }

        assertEquals(2, data.delete("app:ideas", "ideas") { it.getValue("name") != "b" })
        assertEquals(listOf("b"), data.items("app:ideas", "ideas").map { it.getValue("name") })
        assertEquals(0, data.delete("app:ideas", "ideas") { false })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun updatesAndDeletesAreAnnouncedButNotDeletesOfNothing() = runTest {
        data.add("app:ideas", "ideas", named("Dark mode"))
        val changes = mutableListOf<AppData.Change>()
        val listening = launch(UnconfinedTestDispatcher(testScheduler)) { data.changes.toList(changes) }

        data.update("app:ideas", "ideas", "id1", named("Light mode"))
        data.delete("app:ideas", "ideas") { false }
        data.delete("app:ideas", "ideas") { true }

        assertEquals(2, changes.size)
        listening.cancel()
    }
}
