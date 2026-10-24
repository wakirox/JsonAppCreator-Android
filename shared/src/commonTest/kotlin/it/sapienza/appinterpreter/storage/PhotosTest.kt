package it.sapienza.appinterpreter.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PhotosTest {
    @Test
    fun aReferenceNamesAFileInThePhotosFolderOnly() {
        assertEquals("a1.jpg", Photos.fileName(Photos.reference("a1.jpg")))
        assertNull(Photos.fileName("https://example.com/a.jpg"))
        assertNull(Photos.fileName("appkin-photo:"))
        assertNull(Photos.fileName("appkin-photo:../../shared_prefs/AppData.xml"), "no way out of the folder")
        assertNull(Photos.fileName("appkin-photo:.."))
    }

    @Test
    fun referencesAreFoundAnywhereInSavedText() {
        val saved = """[{"title":"Tile","photos":["appkin-photo:a-1.jpg","appkin-photo:b_2.jpg"]},{"title":"x"}]"""
        assertEquals(setOf("appkin-photo:a-1.jpg", "appkin-photo:b_2.jpg"), Photos.referencesIn(saved))
    }
}
