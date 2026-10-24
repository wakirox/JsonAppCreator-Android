package it.sapienza.androidratio.appratio

import it.sapienza.appinterpreter.storage.MemoryAppStore
import it.sapienza.appinterpreter.utils.ImageCacheExpiry
import org.junit.Assert.assertEquals
import org.junit.Test

/** The image cache is emptied every 90 days, as TMDB's six-month limit on cached content asks. */
class ImageCacheExpiryTest {
    private val store = MemoryAppStore()
    private var now = 0L
    private var clears = 0
    private val expiry = ImageCacheExpiry(store, { now }) { clears++ }
    private val day = 24L * 60 * 60 * 1000

    @Test
    fun theFirstCheckStartsTheClockWithoutClearing() {
        expiry.check()
        assertEquals(0, clears)
    }

    @Test
    fun theCacheIsClearedOnceNinetyDaysHavePassedAndTheClockStartsAgain() {
        expiry.check()
        now = 89 * day
        expiry.check()
        assertEquals(0, clears)

        now = 90 * day
        expiry.check()
        assertEquals(1, clears)

        now = 179 * day
        expiry.check()
        assertEquals(1, clears)
        now = 180 * day
        expiry.check()
        assertEquals(2, clears)
    }
}
