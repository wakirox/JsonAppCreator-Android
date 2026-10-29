package it.sapienza.appinterpreter.storage

import it.sapienza.appinterpreter.json.JsonText
import kotlin.test.Test
import kotlin.test.assertEquals

class FilesTest {
    @Test
    fun aFilesValueIsAListOfObjectsWithALink() {
        val value = JsonText.parse(
            """[ { "name": "plan.pdf", "type": "application/pdf", "size": 2048, "uri": "content://docs/1" }, { "name": "no link" }, "text", { "uri": "content://docs/raw/notes.txt" } ]"""
        )
        assertEquals(
            listOf(Files.Attached("plan.pdf", "application/pdf", 2048, "content://docs/1"), Files.Attached("notes.txt", null, null, "content://docs/raw/notes.txt")),
            Files.of(value)
        )
        assertEquals(Files.of(value).first(), Files.of(Files.of(value).first().toJson()).single(), "written and read back")
        assertEquals(emptyList(), Files.of(null))
    }

    @Test
    fun sizesAndSafeNames() {
        assertEquals(listOf("820 B", "12 KB", "2.4 MB"), listOf(820L, 12_300L, 2_430_000L).map(Files::sizeText))
        assertEquals("Plan-v2-final-.pdf", Files.safeName("Plan v2 (final).pdf"))
        assertEquals("città.txt", Files.safeName("città.txt"))
        assertEquals("file", Files.safeName("??"))
    }
}
