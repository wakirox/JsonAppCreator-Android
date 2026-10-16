package it.sapienza.appinterpreter.security

import android.content.Context
import android.net.Uri
import it.sapienza.appinterpreter.utils.DomainController
import org.json.JSONObject
import java.util.Base64

object ConfigurationApiKeys {
    private lateinit var applicationContext: Context
    private lateinit var encryptedStore: EncryptedApiKeyStore
    private var bundledValues: Map<String, Map<String, String>> = emptyMap()

    fun initialize(context: Context, encodedBundledValues: String) {
        applicationContext = context.applicationContext
        encryptedStore = EncryptedApiKeyStore(applicationContext)
        bundledValues = decodeBundledValues(encodedBundledValues)
    }

    fun placeholderNames(json: String): Set<String> = ApiKeyPlaceholders.names(json)

    fun hasSavedValue(json: String, name: String): Boolean =
        encryptedStore.read(ApiKeyPlaceholders.configurationId(json), name) != null

    fun save(json: String, values: Map<String, String>) {
        save(ApiKeyPlaceholders.configurationId(json), json, values)
    }

    fun saveForCurrentConfiguration(json: String, values: Map<String, String>) {
        save(DomainController.configurationId(applicationContext), json, values)
    }

    fun availableNamesForCurrentConfiguration(json: String): Set<String> {
        val configurationId = DomainController.configurationId(applicationContext)
        val bundledConfiguration = bundledName(configurationId)?.let(bundledValues::get).orEmpty()
        return ApiKeyPlaceholders.names(json).filterTo(mutableSetOf()) { name ->
            name in bundledConfiguration || encryptedStore.read(configurationId, name) != null
        }
    }

    private fun save(configurationId: String, json: String, values: Map<String, String>) {
        values.forEach { (name, value) ->
            require(name in ApiKeyPlaceholders.names(json)) {
                "Cannot save an API key that is not declared by the configuration"
            }
            if (value.isNotEmpty()) encryptedStore.write(configurationId, name, value)
        }
    }

    fun replaceForCurrentConfiguration(text: String): String {
        check(::applicationContext.isInitialized) { "ConfigurationApiKeys has not been initialized" }
        val configurationId = DomainController.configurationId(applicationContext)
        val names = ApiKeyPlaceholders.names(text)
        if (names.isEmpty()) return text

        val values = buildMap {
            bundledName(configurationId)?.let { putAll(bundledValues[it].orEmpty()) }
            names.forEach { name ->
                encryptedStore.read(configurationId, name)?.let { put(name, it) }
            }
        }.mapValues { (_, value) -> Uri.encode(value) }
        return ApiKeyPlaceholders.replace(text, values)
    }

    internal fun decodeBundledValues(encoded: String): Map<String, Map<String, String>> {
        if (encoded.isBlank()) return emptyMap()
        val root = JSONObject(String(Base64.getDecoder().decode(encoded), Charsets.UTF_8))
        return root.keys().asSequence().associateWith { configurationName ->
            val configuration = root.getJSONObject(configurationName)
            configuration.keys().asSequence().associateWith(configuration::getString)
        }
    }

    private fun bundledName(configurationId: String): String? =
        configurationId.removePrefix(BUNDLED_PREFIX).takeIf { configurationId.startsWith(BUNDLED_PREFIX) }

    private const val BUNDLED_PREFIX = "bundled:"
}
