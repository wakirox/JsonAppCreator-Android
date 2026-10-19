package it.sapienza.appinterpreter.editor

import it.sapienza.appinterpreter.json.JsonText

/**
 * [json] re-indented with two spaces, keys in their order, numbers as written (0.10 stays 0.10) and without escaped
 * slashes. Unchanged if it isn't valid JSON, or has a key twice (formatting would silently keep only one).
 */
fun prettyJson(json: String): String = runCatching {
    JsonText.parse(json, strictDuplicates = true)?.let(JsonText::pretty) ?: json
}.getOrDefault(json)
