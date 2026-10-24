package it.sapienza.appinterpreter.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.content.FileProvider
import it.sapienza.appinterpreter.storage.PhotoReader
import it.sapienza.appinterpreter.storage.Photos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * The photos JSON apps keep, in the app's private storage: each picked photo is copied in (scaled down to at most
 * [MAX_SIDE] pixels, as JPEG), so it stays after the gallery's copy goes, and is referred to as "appkin-photo:<name>".
 * They're backed up with the app's data, whose cloud backup must stay under 25 MB, so they're kept small: a photo is
 * around 300 KB.
 */
class PhotoStore(context: Context) {
    private val resolver = context.contentResolver
    private val folder = File(context.filesDir, "photos")

    /** Copies the photo at [uri] in and gives its reference. */
    suspend fun add(uri: Uri): String = withContext(Dispatchers.IO) {
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            val (width, height) = info.size.width to info.size.height
            val scale = MAX_SIDE.toFloat() / maxOf(width, height)
            if (scale < 1) decoder.setTargetSize((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1))
        }
        folder.mkdirs()
        val name = "${UUID.randomUUID()}.jpg"
        File(folder, name).outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, it)) }
        Photos.reference(name)
    }

    /** A kept photo's bytes, for a request that sends it. */
    val reader = PhotoReader { reference -> withContext(Dispatchers.IO) { file(reference)?.takeIf(File::isFile)?.readBytes() } }

    /** A content address another app can read (the FileProvider's), for sharing a kept photo; null when it's gone. */
    fun shareable(context: Context, reference: String): Uri? =
        file(reference)?.takeIf(File::isFile)?.let { FileProvider.getUriForFile(context, "${context.packageName}.files", it) }

    /** The file a reference points to, or null when [reference] isn't a photo. */
    fun file(reference: String): File? = Photos.fileName(reference)?.let { File(folder, it) }

    /**
     * Deletes the photos no saved data refers to ([referenced]) that are older than [graceMillis], so a photo picked
     * in a form not yet saved survives.
     */
    fun deleteUnused(referenced: Set<String>, now: Long = System.currentTimeMillis(), graceMillis: Long = DAY): Int =
        folder.listFiles().orEmpty().count { file ->
            Photos.reference(file.name) !in referenced && now - file.lastModified() > graceMillis && file.delete()
        }

    companion object {
        const val MAX_SIDE = 1600
        private const val QUALITY = 80
        private const val DAY = 24L * 60 * 60 * 1000
    }
}
