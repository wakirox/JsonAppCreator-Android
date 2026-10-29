package it.sapienza.appinterpreter.runtime

import kotlin.test.Test
import kotlin.test.assertEquals

class TagSuggestionsTest {
    private val known = listOf("watch", "ui", "widgets", "Notes app", "alerts")

    @Test
    fun withNothingTypedEveryUnusedTagAlphabetically() {
        assertEquals(
            listOf("alerts", "Notes app", "widgets"),
            TagSuggestions.matching(known, current = listOf("UI", "watch"), typed = "")
        )
    }

    @Test
    fun tagsStartingWithTheTextComeFirstThenOnesContainingIt() {
        assertEquals(listOf("watch", "widgets"), TagSuggestions.matching(known, emptyList(), "w"))
        assertEquals(listOf("alerts"), TagSuggestions.matching(known, emptyList(), "#AL"), "a # and case aside")
        assertEquals(listOf("Notes app"), TagSuggestions.matching(known, emptyList(), "NOTE"))
    }

    @Test
    fun aTagTypedInFullOrAlreadyAddedIsNotOfferedAndAtMostEightShow() {
        assertEquals(emptyList(), TagSuggestions.matching(known, emptyList(), "ui"))
        assertEquals(listOf("widgets"), TagSuggestions.matching(known, listOf("watch"), "w"))
        assertEquals(TagSuggestions.MAX, TagSuggestions.matching((1..20).map { "t$it" }, emptyList(), "").size)
    }
}
