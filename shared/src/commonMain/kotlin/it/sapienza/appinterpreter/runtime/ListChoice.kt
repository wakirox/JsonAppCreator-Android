package it.sapienza.appinterpreter.runtime

/** What the user chose above a stored list: one of its sort options (by position), and a tag to show only. */
data class ListChoice(val sortOption: Int = 0, val tag: String? = null)
