package it.sapienza.appinterpreter.extensions

import android.content.Intent
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

const val DEFAULT_NAME = "object"

object IntentUtil {
    @Suppress("SpellCheckingInspection")
    val jackson = jacksonObjectMapper()
}

fun Intent.putExtraJson(name: String, src: Any) {
    putExtra(name, IntentUtil.jackson.writeValueAsString(src))
}

fun Intent.putExtraJson(src: Any) {
    putExtra(DEFAULT_NAME, IntentUtil.jackson.writeValueAsString(src))
}

fun <T> Intent.getJsonExtra(name: String, `class`: Class<T>): T? {
    val stringExtra = getStringExtra(name)
    if (stringExtra != null) {
        return IntentUtil.jackson.readValue<T>(stringExtra, `class`)
    }
    return null
}

fun <T> Intent.getJsonExtra(`class`: Class<T>): T? {
    val stringExtra = getStringExtra(DEFAULT_NAME)
    if (stringExtra != null) {
        return IntentUtil.jackson.readValue<T>(stringExtra, `class`)
    }
    return null
}

fun <T> Intent.getJsonExtra(ref : TypeReference<T>): T? {
    val stringExtra = getStringExtra(DEFAULT_NAME)
    if (stringExtra != null) {
        return IntentUtil.jackson.readValue<T>(stringExtra, ref)
    }
    return null
}