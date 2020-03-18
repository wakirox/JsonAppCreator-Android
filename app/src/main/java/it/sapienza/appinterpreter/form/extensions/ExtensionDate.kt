package it.sapienza.appinterpreter.form.extensions

import java.text.SimpleDateFormat
import java.util.*

fun Date.stringValue() : String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(this)
}