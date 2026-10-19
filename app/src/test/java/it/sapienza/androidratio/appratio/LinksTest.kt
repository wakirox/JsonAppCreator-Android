package it.sapienza.androidratio.appratio

import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import it.sapienza.appinterpreter.ui.linkified
import it.sapienza.appinterpreter.ui.urlRanges
import org.junit.Assert.assertEquals
import org.junit.Test

/** Web addresses in a text view become links. */
class LinksTest {

    private fun urls(text: String) = urlRanges(text).map { text.substring(it.first, it.last + 1) }

    @Test
    fun findsAddressesWithoutTheSentencesPunctuation() {
        assertEquals(
            listOf("https://www.themoviedb.org/", "http://example.com/a?b=1&c=2"),
            urls("Data from https://www.themoviedb.org/. See also (http://example.com/a?b=1&c=2), thanks.")
        )
    }

    @Test
    fun ignoresTextWithoutAddresses() {
        assertEquals(emptyList<String>(), urls("No links here, not even https:// alone."))
    }

    @Test
    fun keepsTheTextAndLinksEachAddress() {
        val text = "Not endorsed by TMDB.\nhttps://www.themoviedb.org/"
        val result = linkified(text, TextLinkStyles())
        assertEquals(text, result.text)
        val links = result.getLinkAnnotations(0, text.length)
        assertEquals(listOf("https://www.themoviedb.org/"), links.map { (it.item as LinkAnnotation.Url).url })
        assertEquals(text.indexOf("https"), links.single().start)
    }
}
