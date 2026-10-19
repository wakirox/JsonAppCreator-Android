package it.sapienza.androidratio.appratio

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import it.sapienza.appinterpreter.utils.SharedApps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

/**
 * The websites a shared app lists are found in common code. Here they're checked against the way the app found them
 * before, with Jackson and java.net.URI, on every bundled configuration and on awkward addresses.
 */
class SharedAppsParityTest {
    private val mapper = ObjectMapper()

    private fun reference(json: String): List<String> {
        val root = runCatching { mapper.readTree(json) }.getOrNull() ?: return emptyList()
        val hosts = sortedSetOf<String>()
        fun visit(node: JsonNode) {
            when {
                node.isTextual -> referenceHost(node.asText())?.let(hosts::add)
                node.isContainerNode -> node.forEach(::visit)
            }
        }
        visit(root)
        return hosts.toList()
    }

    private fun referenceHost(text: String): String? {
        val value = text.trim()
        if (!value.startsWith("https://", ignoreCase = true) && !value.startsWith("http://", ignoreCase = true)) return null
        val authority = value.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#')
        return runCatching { URI("https://$authority").host }.getOrNull()?.lowercase()?.takeIf { it.isNotEmpty() }
    }

    @Test
    fun everyBundledConfigurationListsTheSameWebsites() {
        val configurations = listOf(Path.of("src", "main", "res", "raw"), Path.of("..", "samples"), Path.of("..", "examples"))
            .flatMap { directory -> Files.list(directory).use { it.toList() } }
            .filter { it.name.endsWith(".json") }
        assertTrue(configurations.size >= 10)
        configurations.forEach { file ->
            val json = Files.readString(file)
            assertEquals(file.toString(), reference(json), SharedApps.webAddresses(json))
        }
    }

    @Test
    fun awkwardAddressesGiveTheSameHosts() {
        listOf(
            "https://Upload.Wikimedia.org/a.jpg", "http://example.com:8080/x", "https://ann@user.example.com:443/x",
            "http://[::1]:8080/", "http://10.0.2.2/api", " HTTPS://Example.COM?q=1 ", "https://example.com#top",
            "https://", "https://${'$'}{host}/x", "https://under_score.com/", "https://example.com:http/",
            "https://-bad.com/", "https://a b.com/", "https://example.com./", "https://xn--bcher-kva.example/",
            "https://1.2.3.4:99", "https://ex%41mple.com/", "ftp://example.com", "see https://example.com"
        ).forEach { address ->
            assertEquals(address, referenceHost(address), SharedApps.webAddresses("[${mapper.writeValueAsString(address)}]").singleOrNull())
        }
    }
}
