package it.sapienza.androidratio.appratio

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** Android's backup takes what people make (saved data, photos, their apps) and never the encrypted API keys. */
class BackupRulesTest {
    private val rules = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(File("src/main/res/xml/data_extraction_rules.xml")).documentElement

    private fun included(section: String): Set<String> {
        val element = rules.getElementsByTagName(section).item(0) as Element
        val includes = element.getElementsByTagName("include")
        return (0 until includes.length).map { includes.item(it) as Element }
            .map { "${it.getAttribute("domain")}:${it.getAttribute("path")}" }.toSet()
    }

    @Test
    fun cloudBackupAndANewPhoneGetTheSameThings() {
        assertEquals(included("cloud-backup"), included("device-transfer"))
    }

    @Test
    fun savedDataPhotosAndAppsAreBackedUp() {
        val backedUp = included("cloud-backup")
        listOf("sharedpref:AppData.xml", "sharedpref:AppLibrary.xml", "sharedpref:DomainController.xml", "file:photos/")
            .forEach { assertTrue("$it is backed up", it in backedUp) }
    }

    @Test
    fun theEncryptedApiKeysAreNot() {
        // Their key is in this phone's Keystore: restored elsewhere they couldn't be read.
        assertFalse(included("cloud-backup").any { "encrypted" in it })
        assertTrue("only listed files go, so the keys can't slip in", rules.getElementsByTagName("exclude").length == 0)
    }

    @Test
    fun theManifestUsesTheseRules() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android:allowBackup=\"true\""))
        assertTrue(manifest.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\""))
    }
}
