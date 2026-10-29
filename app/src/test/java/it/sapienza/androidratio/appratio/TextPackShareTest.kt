package it.sapienza.androidratio.appratio

import it.sapienza.appinterpreter.runtime.usecase.TextPackUseCase
import it.sapienza.appinterpreter.storage.PhotoReader
import it.sapienza.appinterpreter.utils.TextPackShare
import java.util.zip.ZipFile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** A TextPack is a zip of one TextBundle folder: info.json, text.markdown and the photos it links in assets/. */
class TextPackShareTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun theZipHoldsTheBundleWithItsTextInfoAndPhotos() = runTest {
        val pack = TextPackUseCase.Pack(
            "Rainy: open ideas",
            "# Rainy\n\n![](assets/dark-mode-1.jpg)\n",
            listOf(
                TextPackUseCase.Asset("assets/dark-mode-1.jpg", "appkin-photo:a.jpg"),
                TextPackUseCase.Asset("assets/gone-1.jpg", "appkin-photo:gone.jpg"),
                TextPackUseCase.Asset("assets/dark-mode-plan.pdf", "content://docs/plan")
            )
        )
        val photos = PhotoReader { reference -> if (reference == "appkin-photo:a.jpg") byteArrayOf(1, 2, 3) else null }
        val file = folder.newFile("Rainy open ideas.zip")

        TextPackShare.write(pack, "Rainy open ideas", photos, { uri ->
            if (uri ==
                "content://docs/plan"
            ) {
                byteArrayOf(9)
            } else {
                null
            }
        }, file)

        ZipFile(file).use { zip ->
            assertEquals(
                "a photo no longer there is left out; a file is packed as it is",
                listOf(
                    "Rainy open ideas.textbundle/info.json",
                    "Rainy open ideas.textbundle/text.markdown",
                    "Rainy open ideas.textbundle/assets/dark-mode-1.jpg",
                    "Rainy open ideas.textbundle/assets/dark-mode-plan.pdf"
                ),
                zip.entries().toList().map { it.name }
            )
            fun text(name: String) = zip.getInputStream(zip.getEntry("Rainy open ideas.textbundle/$name")).readBytes()
            assertEquals(pack.markdown, String(text("text.markdown")))
            assertEquals(TextPackUseCase.INFO, String(text("info.json")))
            assertEquals(listOf<Byte>(1, 2, 3), text("assets/dark-mode-1.jpg").toList())
            assertEquals(
                "an attached file goes in as it is",
                listOf<Byte>(9),
                text("assets/dark-mode-plan.pdf").toList()
            )
        }
    }
}
