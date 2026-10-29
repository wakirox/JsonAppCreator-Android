package it.sapienza.appinterpreter.storage

/**
 * Photos a JSON app keeps on the phone, picked in a photos field: each is referred to as "appkin-photo:<name>", which
 * an image view shows like an address, and the platform keeps the file in the app's private storage.
 */
object Photos {
    const val SCHEME = "appkin-photo:"

    /** How many photos a photos field takes when it doesn't say. */
    const val DEFAULT_MAX = 10

    fun isPhoto(reference: String): Boolean = reference.startsWith(SCHEME)

    /** The file name in a reference, or null when it isn't one (or would leave the photos folder). */
    fun fileName(reference: String): String? = reference.takeIf(::isPhoto)?.removePrefix(SCHEME)?.takeIf {
        it.isNotEmpty() && '/' !in it && '\\' !in it &&
            it != ".."
    }

    fun reference(fileName: String): String = SCHEME + fileName

    /** The photo references anywhere in [text] (saved data, say), to tell which files are still used. */
    fun referencesIn(text: String): Set<String> = REFERENCE.findAll(text).map { it.value }.toSet()

    private val REFERENCE = Regex("appkin-photo:[A-Za-z0-9._-]+")
}

/** Reads a kept photo's JPEG bytes by its reference (null when there is no such photo): the platform's PhotoStore. */
fun interface PhotoReader {
    suspend fun jpeg(reference: String): ByteArray?

    companion object {
        /** No photos to read: references are sent as they are. */
        val None = PhotoReader { null }
    }
}
