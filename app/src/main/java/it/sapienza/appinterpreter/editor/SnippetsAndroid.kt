package it.sapienza.appinterpreter.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** [Snippets.autoIndent] on the editor's text field value. */
fun Snippets.withAutoIndent(old: TextFieldValue, new: TextFieldValue): TextFieldValue =
    autoIndent(old.text, new.text, new.selection.start, new.selection.collapsed)
        ?.let { (text, cursor) -> TextFieldValue(text, TextRange(cursor)) } ?: new
