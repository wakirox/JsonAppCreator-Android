package it.sapienza.appinterpreter.runtime.usecase

import it.sapienza.appinterpreter.json.filledLines
import it.sapienza.appinterpreter.json.getValue
import it.sapienza.appinterpreter.json.matchReplace
import it.sapienza.appinterpreter.model.event.ItemsExport
import it.sapienza.appinterpreter.json.valueAt
import it.sapienza.appinterpreter.storage.Files
import it.sapienza.appinterpreter.storage.Photos
import kotlinx.serialization.json.JsonObject

/**
 * What a TextPack (textbundle.org: a zipped TextBundle) of saved objects holds: Markdown with the title, then each
 * object's text, its photos and its attached files, linked as assets/… files named after the object; the phone writes
 * the zip. A photo that is a web address stays one.
 */
class TextPackUseCase {
    /**
     * A file of the pack: [path] inside the bundle ("assets/dark-mode-1.jpg") and what it holds, a kept photo's
     * reference or an attached file's link.
     */
    class Asset(val path: String, val reference: String) {
        val isPhoto: Boolean get() = Photos.isPhoto(reference)
    }

    /** [title] names the pack; [markdown] is its text.markdown, [assets] its files. */
    class Pack(val title: String, val markdown: String, val assets: List<Asset>)

    operator fun invoke(event: ItemsExport, items: List<JsonObject>, data: JsonObject?, fallbackTitle: String): Pack {
        val title = event.title?.matchReplace(data)?.trim()?.takeIf { it.isNotEmpty() } ?: fallbackTitle
        val assets = mutableListOf<Asset>()
        val parts = items.mapIndexed { index, item ->
            val name = slug(item.getValue("title") ?: item.getValue("name")).ifEmpty { "item-${index + 1}" }
            val pictures = event.photos?.let { PdfPageUseCase.photosAt(item, it) }.orEmpty().mapIndexed { n, photo ->
                if (Photos.isPhoto(photo)) {
                    val path = unique("assets/$name-${n + 1}.jpg", assets)
                    assets += Asset(path, photo)
                    "![]($path)"
                } else {
                    "![]($photo)"
                }
            }
            val attached = event.files?.let { Files.of(item.valueAt(it)) }.orEmpty().map { file ->
                val path = unique("assets/$name-${Files.safeName(file.name)}", assets)
                assets += Asset(path, file.uri)
                "📎 [${file.name}]($path)"
            }
            (listOf(event.text.filledLines(item)) + pictures + attached.joinToString("\n").ifEmpty { null }).filterNotNull()
                .filter { it.isNotBlank() }.joinToString("\n\n")
        }
        val empty = event.emptyText?.matchReplace(data)?.takeIf { items.isEmpty() }
        val body = listOfNotNull("# $title", empty) + parts.joinToString("\n\n---\n\n").takeIf { it.isNotEmpty() }.let(::listOfNotNull)
        return Pack(title, body.joinToString("\n\n") + "\n", assets)
    }

    private fun unique(path: String, taken: List<Asset>): String {
        if (taken.none { it.path == path }) return path
        val dot = path.lastIndexOf('.').takeIf { it > path.lastIndexOf('/') } ?: path.length
        val (stem, extension) = path.substring(0, dot) to path.substring(dot)
        return generateSequence(2) { it + 1 }.map { "$stem-$it$extension" }.first { candidate -> taken.none { it.path == candidate } }
    }

    companion object {
        /** The bundle's info.json (TextBundle version 2, Markdown); transient: made to be handed on. */
        const val INFO = """{"version":2,"type":"net.daringfireball.markdown","transient":true,"creatorIdentifier":"it.sapienza.androidratio.appratio"}"""

        /** [text] as a file name part: lower case letters and digits, joined by hyphens. */
        fun slug(text: String?): String =
            text.orEmpty().lowercase().map { if (it.isLetterOrDigit()) it else '-' }.joinToString("")
                .replace(Regex("-+"), "-").trim('-').take(40).trimEnd('-')
    }
}
