package it.sapienza.androidratio.appratio

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import it.sapienza.appinterpreter.utils.AppLibrary
import it.sapienza.appinterpreter.utils.DomainController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** My apps: apps are saved under their names, the latest version replaces the earlier one, newest first. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppLibraryTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun clear() = context.getSharedPreferences("AppLibrary", Context.MODE_PRIVATE).edit().clear().commit().let { }

    private fun app(name: String, version: String = "1") =
        """{ "schemaVersion": 3, "name": "$name", "version": "$version", "mainView": { "type": "text" } }"""

    @Test
    fun appsAreSavedByNameNewestFirst() {
        AppLibrary.save(context, app("Books"))
        Thread.sleep(5)
        AppLibrary.save(context, app("Kitchen"))
        Thread.sleep(5)
        AppLibrary.save(context, app("Books", version = "2"))

        val apps = AppLibrary.all(context)
        assertEquals(listOf("Books", "Kitchen"), apps.map { it.name })
        assertEquals(app("Books", version = "2"), apps.first().json)
    }

    @Test
    fun runningAnAppSavesItAndDeletingRemovesIt() {
        DomainController.setJsonFile(app("Planner"), context)
        val saved = checkNotNull(AppLibrary.get(context, AppLibrary.idOf("Planner")))
        AppLibrary.delete(context, saved.id)
        assertNull(AppLibrary.get(context, saved.id))
    }

    @Test
    fun withoutANameNothingIsSaved() {
        AppLibrary.save(context, "{ \"schemaVersion\": 3 }")
        AppLibrary.save(context, "not json")
        assertEquals(emptyList<Any>(), AppLibrary.all(context))
    }
}
