package it.sapienza.androidratio.appratio

import it.sapienza.appinterpreter.utils.AppIcon
import it.sapienza.appinterpreter.utils.AppLibrary
import it.sapienza.appinterpreter.utils.AppParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppIconTest {
    private val json = """{ "name": "Top Motorcycles", "icon": "🏍️", "version": "1.0", "mainView": { "type": "text", "title": "Hi" } }"""

    @Test
    fun anAppsEmojiIsItsIcon() {
        assertEquals("🏍️", AppParser(json).parseApplication2().icon)
        assertEquals("🏍️", AppLibrary.iconOf(json))
    }

    @Test
    fun withoutAnEmojiTheIconIsTheFirstLetter() {
        assertNull(AppLibrary.iconOf("""{ "name": "Book club" }"""))
        assertNull(AppIcon.emoji("  "))
        assertEquals("B", AppIcon.letter("  book club"))
    }

    @Test
    fun theShortcutBitmapIsDrawnEitherWay() {
        assertEquals(432, AppIcon.bitmap("Top Motorcycles", "🏍️").width)
        assertEquals(432, AppIcon.bitmap("Book club", null).height)
    }
}
