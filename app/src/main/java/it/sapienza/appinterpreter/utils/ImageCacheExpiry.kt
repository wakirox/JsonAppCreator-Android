package it.sapienza.appinterpreter.utils

import it.sapienza.appinterpreter.storage.AppStore

/**
 * Empties the image cache every [maxAgeMillis], so no downloaded image is kept for long: TMDB's terms allow caching
 * its content for at most six months. The first check only starts the clock.
 */
class ImageCacheExpiry(
    private val store: AppStore,
    private val now: () -> Long = System::currentTimeMillis,
    private val maxAgeMillis: Long = MAX_AGE_MILLIS,
    private val clear: () -> Unit
) {
    /** Clears the cache when it was last cleared (or first checked) more than [maxAgeMillis] ago. */
    fun check() {
        val since = store.get(KEY)?.toLongOrNull()
        if (since != null && now() - since < maxAgeMillis) return
        if (since != null) clear()
        store.put(KEY, now().toString())
    }

    companion object {
        private const val KEY = "image_cache_since"

        /** 90 days: well inside the six months TMDB allows. */
        const val MAX_AGE_MILLIS = 90L * 24 * 60 * 60 * 1000
    }
}
