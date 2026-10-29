package it.sapienza.appinterpreter.utils

import android.app.Activity
import android.content.Intent
import it.sapienza.appinterpreter.runtime.usecase.TextPackUseCase
import it.sapienza.appinterpreter.storage.PhotoReader
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Writes a [TextPackUseCase.Pack] as a .textpack (a zip holding "<title>.textbundle/" with info.json, text.markdown
 * and assets/) and opens the share sheet on it. A photo no longer there is left out of assets/.
 */
class TextPackShare(private val activity: Activity, private val shareTitle: String) {
    suspend fun share(pack: TextPackUseCase.Pack) {
        val name = PdfPrint.fileName(pack.title)
        val file = File(File(activity.cacheDir, "shared/textpack").apply { deleteRecursively(); mkdirs() }, "$name.textpack")
        write(pack, name, PhotoStore(activity).reader, file)
        activity.startActivity(shareIntent(file, name))
    }

    internal fun shareIntent(file: File, name: String): Intent =
        ExternalApps.shareFile(activity, file, MIME_TYPE, "$name.textpack", shareTitle)

    companion object {
        /** A TextPack is a zip; apps that take one ask for zips. */
        const val MIME_TYPE = "application/zip"

        /** Zips [pack] into [file]: the bundle folder [name].textbundle, its info.json, text.markdown and assets. */
        suspend fun write(pack: TextPackUseCase.Pack, name: String, photos: PhotoReader, file: File) = withContext(Dispatchers.IO) {
            val bundle = "$name.textbundle/"
            ZipOutputStream(file.outputStream().buffered()).use { zip ->
                fun put(path: String, bytes: ByteArray) {
                    zip.putNextEntry(ZipEntry(bundle + path))
                    zip.write(bytes)
                    zip.closeEntry()
                }
                put("info.json", TextPackUseCase.INFO.toByteArray())
                put("text.markdown", pack.markdown.toByteArray())
                pack.assets.forEach { asset -> photos.jpeg(asset.reference)?.let { put(asset.path, it) } }
            }
        }
    }
}
