package it.sapienza.appinterpreter.extensions

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

fun String.matchReplace(obj : JSONObject?) : String{
    if(obj == null)return this

    val rx = "(\\$\\{[^}]+\\})"

    val p: Pattern = Pattern.compile(rx)
    val m: Matcher = p.matcher(this)

    var string = this

    while (m.find()) { // Avoids throwing a NullPointerException in the case that you
        m.group(1)?.let {
            obj.getValue(it.substring(2,it.length-1))?.let {value ->
                string = string.replace(it,value)
            } ?: kotlin.run { string = string.replace(it,"") }
        }
    }

    return string
}

fun String.failedMatch(obj : JSONObject?) : Boolean{
    if(obj == null)return false

    val rx = "(\\$\\{[^}]+\\})"

    val p: Pattern = Pattern.compile(rx)
    val m: Matcher = p.matcher(this)

    while (m.find()) { // Avoids throwing a NullPointerException in the case that you
        m.group(1)?.let {
            obj.getValue(it.substring(2,it.length-1))?.let {value ->

            } ?: kotlin.run { return true }
        }
    }

    return false
}

fun String.toDate() : Date? {
    return SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN).parse(this)
}