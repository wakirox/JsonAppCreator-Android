package it.sapienza.androidratio.appratio

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import it.sapienza.appinterpreter.ui.PhotoZoom
import org.junit.Assert.assertEquals
import org.junit.Test

/** A full-screen photo follows the finger at any zoom, and stays on the screen. */
class PhotoZoomTest {
    private val screen = IntSize(1000, 2000)

    @Test
    fun fullyZoomedTheFingerMovesThePhotoAsFarAsItGoes() {
        val (scale, offset) = PhotoZoom.next(6f, Offset.Zero, zoom = 1f, pan = Offset(300f, -200f), size = screen)
        assertEquals(6f, scale)
        assertEquals(Offset(300f, -200f), offset)
    }

    @Test
    fun thePhotoCantBeDraggedPastItsEdge() {
        // At 2× the photo is 2000 wide on a 1000 wide screen: 500 to spare on each side.
        val (_, offset) = PhotoZoom.next(2f, Offset(400f, 0f), zoom = 1f, pan = Offset(300f, -5000f), size = screen)
        assertEquals(Offset(500f, -1000f), offset)
    }

    @Test
    fun zoomStaysBetweenFullSizeAndSixTimesAndFullSizeIsCentred() {
        assertEquals(PhotoZoom.MAX_SCALE, PhotoZoom.next(5f, Offset.Zero, zoom = 3f, pan = Offset.Zero, size = screen).first)
        val (scale, offset) = PhotoZoom.next(2f, Offset(400f, 300f), zoom = 0.1f, pan = Offset.Zero, size = screen)
        assertEquals(1f, scale)
        assertEquals(Offset.Zero, offset)
    }
}
