package it.sapienza.appinterpreter.utils

import android.content.Context
import java.io.File

/**
 * Hands large text, such as an app's JSON, to another activity. An Intent holds about 1 MB at most, so the text goes
 * through a file in the app's cache and only the path travels in the Intent.
 */
object TextHandover {
    fun write(context: Context, text: String): String =
        File(File(context.cacheDir, "incoming").apply { mkdirs() }, "text-${System.nanoTime()}.txt")
            .apply { writeText(text) }.absolutePath

    fun read(path: String): String? = runCatching { File(path).readText() }.getOrNull()
}
