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
 * Writes a [TextPackUseCase.Pack] as a TextPack (a zip holding "<title>.textbundle/" with info.json, text.markdown
 * and assets/) and opens the share sheet on it. A photo or file no longer there is left out of assets/.
 */
/*
 * Named .zip, not .textpack: no Android app opens a .textpack, while every Files app unzips a .zip and Obsidian's
 * importer takes a zip of TextBundles on any device.
 */
class TextPackShare(private val activity: Activity, private val shareTitle: String) {
    suspend fun share(pack: TextPackUseCase.Pack) {
        val name = PdfPrint.fileName(pack.title)
        val file = File(
            File(activity.cacheDir, "shared/textpack").apply {
                deleteRecursively()
                mkdirs()
            },
            "$name.zip"
        )
        write(pack, name, PhotoStore(activity).reader, FileAttachments(activity)::read, file)
        activity.startActivity(shareIntent(file, name))
    }

    internal fun shareIntent(file: File, name: String): Intent =
        ExternalApps.shareFile(activity, file, MIME_TYPE, "$name.zip", shareTitle)

    companion object {
        /** A TextPack is a zip; apps that take one ask for zips. */
        const val MIME_TYPE = "application/zip"

        /**
         * Zips [pack] into [file]: the bundle folder [name].textbundle, its info.json, text.markdown and assets, kept
         * photos read by [photos] and attached files by [files].
         */
        suspend fun write(
            pack: TextPackUseCase.Pack,
            name: String,
            photos: PhotoReader,
            files: suspend (String) -> ByteArray?,
            file: File
        ) = withContext(Dispatchers.IO) {
            val bundle = "$name.textbundle/"
            ZipOutputStream(file.outputStream().buffered()).use { zip ->
                fun put(path: String, bytes: ByteArray) {
                    zip.putNextEntry(ZipEntry(bundle + path))
                    zip.write(bytes)
                    zip.closeEntry()
                }
                put("info.json", TextPackUseCase.INFO.toByteArray())
                put("text.markdown", pack.markdown.toByteArray())
                pack.assets.forEach { asset ->
                    val bytes = if (asset.isPhoto) photos.jpeg(asset.reference) else files(asset.reference)
                    bytes?.let { put(asset.path, it) }
                }
            }
        }
    }
}
