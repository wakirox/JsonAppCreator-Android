package it.sapienza.appinterpreter.extensions

import java.text.SimpleDateFormat
import java.util.*

fun String.toDate(): Date? =
    runCatching { SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).apply { isLenient = false }.parse(this) }.getOrNull()
