package it.sapienza.appinterpreter.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import it.sapienza.appinterpreter.storage.PhotoReader
import it.sapienza.appinterpreter.storage.Photos
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The photos JSON apps keep, referred to as "appkin-photo:<name>". A photo picked from the gallery isn't copied: the
 * app keeps a link to it (a lasting read permission) and its own small thumbnail ([THUMB_SIDE] px), which lists show,
 * the backup carries and which stands in once the original is gone (deleted, or on a new phone). When Android won't
 * keep the link (an app may hold only so many), a copy of the photo ([MAX_SIDE] px) is kept instead. Photos kept before
 * links (a full copy as the thumbnail) still work.
 */
class PhotoStore(context: Context) {
    private val context = context.applicationContext
    private val resolver = this.context.contentResolver
    private val folder = File(this.context.filesDir, "photos")
    private val copies = File(folder, "full")
    private val links = SharedPreferencesStore(this.context, LINKS)

    /** Keeps the photo at [uri]: a link to it and a thumbnail (or, without a link, a copy), and gives its reference. */
    suspend fun add(uri: Uri): String = withContext(Dispatchers.IO) {
        val name = "${UUID.randomUUID()}.jpg"
        folder.mkdirs()
        File(folder, name).writeBytes(jpeg(uri, THUMB_SIDE))
        val linked = runCatching {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.isSuccess
        if (linked) {
            links.put(name, uri.toString())
        } else {
            copies.mkdirs()
            File(copies, name).writeBytes(jpeg(uri, MAX_SIDE))
        }
        Photos.reference(name)
    }

    /** The thumbnail a reference points to (what lists show), or null when [reference] isn't a photo. */
    fun file(reference: String): File? = Photos.fileName(reference)?.let { File(folder, it) }

    /** The original photo, while the link to it still reads; null otherwise. */
    private fun original(reference: String): Uri? {
        val uri = Photos.fileName(reference)?.let(links::get)?.toUri() ?: return null
        return uri.takeIf { runCatching { resolver.openInputStream(it)?.close() != null }.getOrDefault(false) }
    }

    /** The largest version there is: the original, else the kept copy, else the thumbnail. */
    private fun best(reference: String): Any? =
        original(reference) ?: Photos.fileName(reference)?.let { File(copies, it) }?.takeIf(File::isFile)
            ?: file(reference)?.takeIf(File::isFile)

    /** What the full-screen view loads for [reference]: the original or the kept copy, else the thumbnail itself. */
    suspend fun fullSize(reference: String): String = withContext(Dispatchers.IO) {
        when (val best = best(reference)) {
            is Uri -> best.toString()
            is File -> best.toUri().toString()
            else -> reference
        }
    }

    /** A photo's bytes for a request that sends it: the original scaled to [MAX_SIDE], else what's kept. */
    val reader = PhotoReader { reference ->
        withContext(Dispatchers.IO) {
            when (val best = best(reference)) {
                is Uri -> runCatching { jpeg(best, MAX_SIDE) }.getOrNull()
                is File -> best.readBytes()
                else -> null
            }
        }
    }

    /**
     * A content address another app can read (the FileProvider's), for sharing: a copy of the best version in the
     * shared cache, as the gallery's own address can't be passed on. Null when nothing is left.
     */
    suspend fun shareable(reference: String): Uri? = withContext(Dispatchers.IO) {
        val name = Photos.fileName(reference) ?: return@withContext null
        val bytes = reader.jpeg(reference) ?: return@withContext null
        val shared = File(context.cacheDir, "shared/photos").apply { mkdirs() }
        val file = File(shared, name).apply { writeBytes(bytes) }
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    /**
     * Forgets the photos no saved data refers to ([referenced]) that are older than [graceMillis], so a photo picked in
     * a form not yet saved survives: their thumbnail, copy and link (and its read permission) go.
     */
    fun deleteUnused(referenced: Set<String>, now: Long = System.currentTimeMillis(), graceMillis: Long = DAY): Int =
        folder.listFiles().orEmpty().filter(File::isFile).count { file ->
            val unused = Photos.reference(file.name) !in referenced && now - file.lastModified() > graceMillis
            if (unused) forget(file.name)
            unused
        }

    private fun forget(name: String) {
        links.get(name)?.let { uri ->
            runCatching { resolver.releasePersistableUriPermission(uri.toUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            links.remove(name)
        }
        File(copies, name).delete()
        File(folder, name).delete()
    }

    /** The image at [uri] as JPEG, scaled to fit [side] pixels (never up). */
    private fun jpeg(uri: Uri, side: Int): ByteArray {
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            val (width, height) = info.size.width to info.size.height
            val scale = side.toFloat() / maxOf(width, height)
            if (scale <
                1
            ) {
                decoder.setTargetSize(
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1)
                )
            }
        }
        return ByteArrayOutputStream().also {
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, it))
        }.toByteArray()
    }

    companion object {
        /** A thumbnail's longest side: enough for lists and a decent fallback full screen. */
        const val THUMB_SIDE = 640

        /** A kept copy's (or an upload's) longest side. */
        const val MAX_SIDE = 1600
        private const val QUALITY = 80
        private const val DAY = 24L * 60 * 60 * 1000

        /** The links to originals, by thumbnail name. Not backed up: a link only reads on the phone that made it. */
        const val LINKS = "PhotoLinks"
    }
}
