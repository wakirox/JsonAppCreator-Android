package it.sapienza.androidratio.appratio

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import it.sapienza.appinterpreter.model.view_model.form_model.helpers.TextType
import it.sapienza.appinterpreter.ui.keyboardOptionsFor
import org.junit.Assert.assertEquals
import org.junit.Test

/** A text field's keyboard: plain text capitalises sentences; addresses, numbers and passwords are left as typed. */
class TextKeyboardTest {
    @Test
    fun plainTextStartsSentencesWithACapital() {
        assertEquals(KeyboardCapitalization.Sentences, keyboardOptionsFor(null).capitalization)
        assertEquals(KeyboardCapitalization.Sentences, keyboardOptionsFor(TextType.plain).capitalization)
        assertEquals(KeyboardType.Text, keyboardOptionsFor(null).keyboardType)
    }

    @Test
    fun addressesNumbersAndPasswordsAreLeftAsTyped() {
        mapOf(
            TextType.email to KeyboardType.Email,
            TextType.url to KeyboardType.Uri,
            TextType.numeric to KeyboardType.Number,
            TextType.password to KeyboardType.Password
        ).forEach { (type, keyboard) ->
            assertEquals(type.name, keyboard, keyboardOptionsFor(type).keyboardType)
            assertEquals(type.name, KeyboardCapitalization.None, keyboardOptionsFor(type).capitalization)
        }
    }
}
