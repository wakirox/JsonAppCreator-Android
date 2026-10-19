package it.sapienza.androidratio.appratio

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import it.sapienza.appinterpreter.editor.JsonColors
import it.sapienza.appinterpreter.editor.Snippets
import it.sapienza.appinterpreter.editor.highlightJson
import it.sapienza.appinterpreter.editor.withAutoIndent
import org.junit.Assert.assertEquals
import org.junit.Test

/** The editor's Android side: syntax colours and the text field's auto-indent. Its logic is tested in :shared. */
class EditorAssistTest {
    @Test
    fun highlightingSurvivesLongStrings() {
        val long = "{\"data\": \"" + "x".repeat(200_000) + "\"}"
        assertEquals(4, highlightJson(long, JsonColors.Light).spanStyles.size) // { key string }
    }

    @Test
    fun enterKeepsTheIndentationInTheTextField() {
        val old = TextFieldValue("  \"views\": []", TextRange(12))
        val typed = Snippets.withAutoIndent(old, TextFieldValue("  \"views\": [\n]", TextRange(13)))
        assertEquals("  \"views\": [\n    \n  ]", typed.text)
        assertEquals(TextRange(typed.text.indexOf("[\n") + 6), typed.selection)
        val pasted = TextFieldValue("x", TextRange(1))
        assertEquals(pasted, Snippets.withAutoIndent(old, pasted))
    }
}
