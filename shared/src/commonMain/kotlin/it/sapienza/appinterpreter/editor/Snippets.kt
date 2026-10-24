package it.sapienza.appinterpreter.editor

/** Ready-made pieces for the editor's Insert menu: a view, a form field or an action, as valid v3 JSON. */
object Snippets {
    /** [formField] snippets go in a form's "formElements"; the others are views. */
    data class Snippet(val label: String, val json: String, val formField: Boolean = false)

    val all = listOf(
        Snippet("Text", """{ "type": "text", "label": "Title", "title": "Some text" }"""),
        Snippet("Image", """{ "type": "image", "url": "https://", "size": { "height": 200 } }"""),
        Snippet(
            "Button with alert",
            """
            {
              "type": "button",
              "title": "Tap me",
              "action": {
                "id": "say_hello",
                "event": { "type": "alert", "value": "Hello!" }
              }
            }
            """.trimIndent()
        ),
        Snippet(
            "List",
            """
            {
              "type": "list",
              "mapping": "items",
              "data": { "items": [ { "name": "First" }, { "name": "Second" } ] },
              "itemView": { "type": "text", "label": "${'$'}{name}" }
            }
            """.trimIndent()
        ),
        Snippet(
            "Form",
            """
            {
              "type": "form",
              "buttonTitle": "Send",
              "formElements": [
                { "type": "text", "mapping": "name", "title": "Name", "mandatory": true }
              ]
            }
            """.trimIndent()
        ),
        Snippet("Text field", """{ "type": "text", "mapping": "field", "title": "Field" }""", formField = true),
        Snippet("Choice field", """{ "type": "selector", "mapping": "choice", "title": "Choice", "options": ["One", "Two"] }""", formField = true),
        Snippet("Time field", """{ "type": "time", "mapping": "time", "title": "Time" }""", formField = true),
        Snippet("Hidden value", """{ "type": "hidden", "mapping": "id", "value": "${'$'}{id}" }""", formField = true),
        Snippet("Row of views", """{ "type": "layout", "orientation": "horizontal", "views": [] }""")
    )

    /** A new app: a title, a line of text and a button, ready to run and to grow. */
    val newApp = """
        {
          "schemaVersion": 3,
          "name": "My app",
          "version": "1.0.0",
          "mainView": {
            "type": "layout",
            "views": [
              {
                "type": "text",
                "label": "My app",
                "title": "Tap + to add views.",
                "textStyle": "bold",
                "textSize": 20
              },
              {
                "type": "button",
                "title": "Say hello",
                "action": {
                  "id": "hello",
                  "event": { "type": "alert", "value": "Hello!" }
                }
              }
            ]
          }
        }
    """.trimIndent()

    /** The indentation of the line holding [offset]. */
    fun indentAt(text: String, offset: Int): String {
        val lineStart = text.lastIndexOf('\n', (offset - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        return text.substring(lineStart).takeWhile { it == ' ' }
    }

    /**
     * [new] (the text after an edit, with the cursor at [cursor]) with a just-typed line break indented like the line
     * above (one level more after { or [), and an empty {} or [] opened onto its own lines: the new text and cursor,
     * or null when the edit wasn't a typed line break.
     */
    fun autoIndent(old: String, new: String, cursor: Int, collapsed: Boolean = true): Pair<String, Int>? {
        val at = cursor
        val typedNewline = collapsed && new.length == old.length + 1 && at > 0 &&
            new[at - 1] == '\n' && new.removeRange(at - 1, at) == old
        if (!typedNewline) return null
        val indent = indentAt(new, at - 1)
        val before = new.getOrNull(at - 2)
        val after = new.getOrNull(at)
        val opens = before == '{' || before == '['
        val inner = if (opens) "$indent  " else indent
        val insert = if (opens && (after == '}' || after == ']')) "$inner\n$indent" else inner
        return new.replaceRange(at, at, insert) to at + inner.length
    }

    /** [json] with every line after the first indented by [indent], to sit where it is inserted. */
    fun indented(json: String, indent: String): String = json.lines().joinToString("\n$indent")
}
