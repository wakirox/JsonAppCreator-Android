package it.sapienza.androidratio.appratio

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import it.sapienza.appinterpreter.activity.EditorActivity
import it.sapienza.appinterpreter.activity.HomeActivity
import it.sapienza.appinterpreter.activity.MainActivity
import it.sapienza.appinterpreter.utils.DomainController
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The raw captures for the Play Store screenshots: drives the real app through each screen and saves them under the
 * app's external files dir, in store/. Run by tools/store/screenshots.sh on a clean install, never in CI.
 */
@RunWith(AndroidJUnit4::class)
class StoreScreenshots {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private val out = File(context.getExternalFilesDir(null), "store")

    @Before
    fun setUp() {
        assumeTrue("Only when screenshots.sh asks for it", InstrumentationRegistry.getArguments().getString("store") == "true")
        out.mkdirs()
    }

    @Test
    fun capture() {
        // Your apps: one the user made with an AI app and one changed from an example. How it works closed.
        val bookClub = instrumentation.context.assets.open("store/book_club.json").bufferedReader().use { it.readText() }
        DomainController.setJsonFile(read(R.raw.v3_race_signup), context)
        DomainController.setJsonFile(bookClub, context)
        context.getSharedPreferences("Home", Context.MODE_PRIVATE).edit().putBoolean("how_it_works_open", false).commit()
        start(HomeActivity::class.java)
        waitFor("Your apps")
        shoot("home")

        DomainController.setJsonFile(R.raw.v3_masterpieces, context)
        start(MainActivity::class.java)
        waitFor("The Great Wave off Kanagawa")
        settle(4000) // the paintings load from the web
        shoot("gallery")

        device.findObject(By.text("Paris Street; Rainy Day")).click()
        waitFor("Art Institute of Chicago")
        settle(3000)
        shoot("detail")

        DomainController.setJsonFile(R.raw.v3_race_signup, context)
        start(MainActivity::class.java)
        waitFor("Sign me up")
        val fields = device.findObjects(By.clazz("android.widget.EditText"))
        fields[0].text = "Giulia Rossi"
        fields[1].text = "giulia@example.com"
        hideKeyboard()
        device.findObject(By.text("10 km")).click()
        device.findObject(By.text("M")).click()
        device.findObjects(By.checkable(true)).last().click() // the switch, after the chips; its label doesn't toggle it
        settle(800)
        shoot("form")

        device.findObject(By.text("Sign me up")).click()
        waitFor("OK")
        settle(800)
        shoot("alert")
        device.findObject(By.text("OK")).click()

        EditorActivity.edit(context.applicationContext.let { ContextWrapperWithNewTask(it) })
        waitFor("Edit this app")
        settle(1500)
        shoot("editor")
    }

    private fun read(resource: Int) = context.resources.openRawResource(resource).bufferedReader().use { it.readText() }

    private fun start(activity: Class<*>) {
        context.startActivity(Intent(context, activity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        device.waitForIdle()
    }

    /** Waits for [text] anywhere on screen; a failure leaves the screen in store/failed.png. */
    private fun waitFor(text: String) {
        if (!device.wait(Until.hasObject(By.textContains(text)), 15_000)) {
            shoot("failed")
            error("\"$text\" didn't appear")
        }
    }

    private fun settle(millis: Long) {
        device.waitForIdle()
        SystemClock.sleep(millis)
    }

    private fun hideKeyboard() {
        if (device.executeShellCommand("dumpsys input_method").contains("mInputShown=true")) device.pressBack()
        settle(500)
    }

    /**
     * Saves the screen with the status bar painted over in the colour just below it: some emulator renderers draw
     * the status bar clipped, and the store shots don't need its icons.
     */
    private fun shoot(name: String) {
        val screen = instrumentation.uiAutomation.takeScreenshot().copy(Bitmap.Config.ARGB_8888, true)
        val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        val bar = if (id != 0) context.resources.getDimensionPixelSize(id) else 0
        val below = IntArray(screen.width).also { screen.getPixels(it, 0, screen.width, 0, bar, screen.width, 1) }
        for (y in 0 until bar) screen.setPixels(below, 0, screen.width, 0, y, screen.width, 1)
        File(out, "$name.png").outputStream().use { screen.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** EditorActivity.edit starts the activity from a plain context, which needs a new task. */
    private class ContextWrapperWithNewTask(base: Context) : android.content.ContextWrapper(base) {
        override fun startActivity(intent: Intent) = super.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
