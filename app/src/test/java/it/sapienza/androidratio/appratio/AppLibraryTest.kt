package it.sapienza.androidratio.appratio

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.utils.AppLibrary
import it.sapienza.appinterpreter.utils.of
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
        AppLibrary.of(context).save(app("Books"), "file:1")
        Thread.sleep(5)
        AppLibrary.of(context).save(app("Kitchen"), "file:2")
        Thread.sleep(5)
        AppLibrary.of(context).save(app("books", version = "2"), "file:3")

        val apps = AppLibrary.of(context).all()
        assertEquals(listOf("books", "Kitchen"), apps.map { it.name })
        assertEquals(app("books", version = "2"), apps.first().json)
    }

    @Test
    fun runningAnAppSavesItWithItsIdentityAndDeletingRemovesIt() {
        DomainController.setJsonFile(app("Planner"), context, "bundled:v3_planner")
        val saved = checkNotNull(AppLibrary.of(context).get(AppLibrary.idOf("Planner")))
        assertEquals("the configuration id is kept for its API keys", "bundled:v3_planner", saved.configurationId)
        AppLibrary.of(context).delete(saved.id)
        assertNull(AppLibrary.of(context).get(saved.id))
    }

    @Test
    fun examplesAreNotSaved() {
        DomainController.setJsonFile(R.raw.v3_masterpieces, context)
        assertEquals(emptyList<Any>(), AppLibrary.of(context).all())
    }

    @Test
    fun aNewAppGetsANameThatIsntTaken() {
        assertEquals("My app", AppLibrary.of(context).freeName("My app"))
        AppLibrary.of(context).save(app("My app"), "file:1")
        AppLibrary.of(context).save(app("My app 2"), "file:2")
        assertEquals("My app 3", AppLibrary.of(context).freeName("My app"))
    }

    @Test
    fun withoutANameNothingIsSaved() {
        assertNull(AppLibrary.of(context).save("{ \"schemaVersion\": 3, \"name\": \"  \" }", "file:1"))
        assertNull(AppLibrary.of(context).save("not json", "file:2"))
        assertEquals(emptyList<Any>(), AppLibrary.of(context).all())
    }
}
