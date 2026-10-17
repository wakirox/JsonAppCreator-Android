package it.sapienza.appinterpreter.editor

/** What is being typed at the cursor: a property name, a value, or neither (outside the root object). */
enum class CursorPosition { KEY, VALUE, NONE }

/** One object or array that contains the cursor, outermost first. */
data class Container(val isObject: Boolean, val start: Int, val slot: Any?)

/**
 * Where the cursor is in a JSON text that may be incomplete or invalid while it is being written.
 *
 * @property containers the objects and arrays around the cursor, root first; [Container.slot] is the key or index
 *   under which each one sits in its parent (null for the root).
 * @property key the property whose value is at the cursor, when [position] is [CursorPosition.VALUE] in an object.
 * @property prefix what has been typed of the current name or value, without the opening quote.
 * @property replaceStart where a suggestion starts replacing: the opening quote, or the first typed character.
 * @property replaceEnd where it stops: after the closing quote when the string is already closed, else the cursor.
 */
data class JsonCursor(
    val containers: List<Container>,
    val position: CursorPosition,
    val key: String?,
    val prefix: String,
    val replaceStart: Int,
    val replaceEnd: Int,
    val inString: Boolean
) {
    companion object {
        fun at(text: String, cursor: Int): JsonCursor {
            class Frame(val isObject: Boolean, val start: Int, val slot: Any?) {
                var expectKey = isObject
                var pendingKey: String? = null
                var currentKey: String? = null
                var index = 0
            }

            val frames = ArrayList<Frame>()
            fun slotForChild(): Any? = frames.lastOrNull()?.let { if (it.isObject) it.currentKey else it.index }

            var i = 0
            var stringStart = -1
            val end = cursor.coerceIn(0, text.length)
            while (i < end) {
                when (val c = text[i]) {
                    '"' -> {
                        val close = closingQuote(text, i, end)
                        if (close < 0) { stringStart = i; break }
                        val top = frames.lastOrNull()
                        if (top != null && top.isObject && top.expectKey) top.pendingKey = text.substring(i + 1, close)
                        i = close
                    }
                    '{', '[' -> frames.add(Frame(c == '{', i, slotForChild()))
                    '}', ']' -> if (frames.isNotEmpty()) frames.removeAt(frames.lastIndex)
                    ':' -> frames.lastOrNull()?.takeIf { it.isObject }?.let {
                        it.currentKey = it.pendingKey
                        it.pendingKey = null
                        it.expectKey = false
                    }
                    ',' -> frames.lastOrNull()?.let {
                        if (it.isObject) { it.expectKey = true; it.currentKey = null } else it.index++
                    }
                }
                i++
            }

            val inString = stringStart >= 0
            val prefixStart = if (inString) stringStart + 1 else {
                var s = end
                while (s > 0 && (text[s - 1].isLetterOrDigit() || text[s - 1] == '_' || text[s - 1] == '-')) s--
                s
            }
            val prefix = text.substring(prefixStart, end)
            val replaceStart = if (inString) stringStart else prefixStart
            val replaceEnd = if (inString) {
                // Replace through the closing quote when it is already there on the same line.
                val lineEnd = text.indexOf('\n', end).let { if (it < 0) text.length else it }
                text.indexOf('"', end).takeIf { it in end until lineEnd }?.plus(1) ?: end
            } else end

            val top = frames.lastOrNull()
            val position = when {
                top == null -> CursorPosition.NONE
                top.isObject && top.expectKey -> CursorPosition.KEY
                else -> CursorPosition.VALUE
            }
            return JsonCursor(
                containers = frames.map { Container(it.isObject, it.start, it.slot) },
                position = position,
                key = top?.takeIf { it.isObject && !it.expectKey }?.currentKey,
                prefix = prefix,
                replaceStart = replaceStart,
                replaceEnd = replaceEnd,
                inString = inString
            )
        }

        /** The index of the quote closing the string opened at [open], or -1 if it isn't closed before [limit]. */
        private fun closingQuote(text: String, open: Int, limit: Int): Int {
            var j = open + 1
            while (j < limit) {
                when (text[j]) {
                    '\\' -> j++
                    '"' -> return j
                    '\n' -> return -1
                }
                j++
            }
            return -1
        }

        /** The index of the brace or bracket closing the one at [open], or -1 if it isn't closed. */
        fun closeOf(text: String, open: Int): Int {
            var depth = 0
            var i = open
            while (i < text.length) {
                when (text[i]) {
                    '{', '[' -> depth++
                    '}', ']' -> { depth--; if (depth == 0) return i }
                    '"' -> { val close = closingQuote(text, i, text.length); if (close < 0) return -1; i = close }
                }
                i++
            }
            return -1
        }

        /**
         * The property names of the object starting at [start] and its "type" value, read to its closing brace (or
         * the end of the text). Strings that aren't closed on their line are skipped.
         */
        fun objectFacts(text: String, start: Int): Pair<Set<String>, String?> {
            val keys = LinkedHashSet<String>()
            var type: String? = null
            var depth = 0
            var expectKey = true
            var lastKey: String? = null
            var i = start
            while (i < text.length) {
                when (text[i]) {
                    '{', '[' -> { depth++; if (depth == 1) expectKey = true }
                    '}', ']' -> { depth--; if (depth == 0) break }
                    ':' -> if (depth == 1) expectKey = false
                    ',' -> if (depth == 1) expectKey = true
                    '"' -> {
                        val close = closingQuote(text, i, text.length)
                        if (close < 0) { i = text.indexOf('\n', i).let { if (it < 0) text.length else it }; continue }
                        val value = text.substring(i + 1, close)
                        if (depth == 1) {
                            if (expectKey) { keys += value; lastKey = value }
                            else if (lastKey == "type") type = value
                        }
                        i = close
                    }
                }
                i++
            }
            return keys to type
        }
    }
}
