package it.sapienza.appinterpreter.runtime.usecase

import it.sapienza.appinterpreter.json.filledLines
import it.sapienza.appinterpreter.json.getValue
import it.sapienza.appinterpreter.json.matchReplace
import it.sapienza.appinterpreter.model.event.ItemsExport
import it.sapienza.appinterpreter.storage.Photos
import kotlinx.serialization.json.JsonObject

/**
 * What a TextPack (textbundle.org: a zipped TextBundle) of saved objects holds: Markdown with the title, then each
 * object's text and its photos, linked as assets/… files named after the object; the phone writes the zip. A photo
 * that is a web address stays one.
 */
class TextPackUseCase {
    /** A file of the pack: [path] inside the bundle ("assets/dark-mode-1.jpg") and the kept photo it holds. */
    class Asset(val path: String, val reference: String)

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
            (listOf(event.text.filledLines(item)) + pictures).filter { it.isNotBlank() }.joinToString("\n\n")
        }
        val empty = event.emptyText?.matchReplace(data)?.takeIf { items.isEmpty() }
        val body = listOfNotNull("# $title", empty) + parts.joinToString("\n\n---\n\n").takeIf { it.isNotEmpty() }.let(::listOfNotNull)
        return Pack(title, body.joinToString("\n\n") + "\n", assets)
    }

    private fun unique(path: String, taken: List<Asset>): String {
        if (taken.none { it.path == path }) return path
        val stem = path.removeSuffix(".jpg")
        return generateSequence(2) { it + 1 }.map { "$stem-$it.jpg" }.first { candidate -> taken.none { it.path == candidate } }
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
