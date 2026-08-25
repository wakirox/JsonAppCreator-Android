import org.gradle.api.GradleException
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun environmentValue(name: String): String? =
    System.getenv(name)?.takeUnless { it.isBlank() }

fun jsonString(value: String): String = buildString {
    append('"')
    value.forEach { character ->
        when (character) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\u000c' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (character.code < 0x20) {
                append("\\u%04x".format(character.code))
            } else {
                append(character)
            }
        }
    }
    append('"')
}

val appVersionName = environmentValue("APP_VERSION_NAME")?.trim() ?: "1.0"
val suppliedVersionCode = environmentValue("APP_VERSION_CODE")?.trim()
val appVersionCode = suppliedVersionCode?.toIntOrNull() ?: if (suppliedVersionCode == null) {
    1
} else {
    throw GradleException("APP_VERSION_CODE must be a numeric Android version code.")
}
if (appVersionCode !in 1..2_100_000_000) {
    throw GradleException("APP_VERSION_CODE must be between 1 and 2100000000.")
}

val environmentConfigApiKeysFile = environmentValue("APP_CONFIG_KEYS_FILE")
val gradleConfigApiKeysFile = providers.gradleProperty("configApiKeysFile").orNull
    ?.takeUnless { it.isBlank() }
val encodedConfigApiKeys = environmentValue("APP_CONFIG_KEYS_BASE64")
val suppliedConfigApiKeySources = listOfNotNull(
    environmentConfigApiKeysFile,
    gradleConfigApiKeysFile,
    encodedConfigApiKeys
).size
if (suppliedConfigApiKeySources > 1) {
    throw GradleException(
        "Use only one of APP_CONFIG_KEYS_FILE/-PconfigApiKeysFile or APP_CONFIG_KEYS_BASE64."
    )
}
val configApiKeysFile = environmentConfigApiKeysFile ?: gradleConfigApiKeysFile

val configApiKeyProperties = Properties()
if (configApiKeysFile != null) {
    val suppliedFile = rootProject.file(configApiKeysFile)
    if (!suppliedFile.isFile) {
        throw GradleException("The configuration API-key properties file does not exist.")
    }
    try {
        suppliedFile.inputStream().use(configApiKeyProperties::load)
    } catch (error: Exception) {
        if (error !is IOException && error !is IllegalArgumentException) throw error
        throw GradleException("The configuration API-key properties file could not be read.")
    }
} else if (encodedConfigApiKeys != null) {
    val decoded = try {
        Base64.getDecoder().decode(encodedConfigApiKeys)
    } catch (_: IllegalArgumentException) {
        throw GradleException("APP_CONFIG_KEYS_BASE64 must be valid Base64-encoded properties data.")
    }
    try {
        ByteArrayInputStream(decoded).use(configApiKeyProperties::load)
    } catch (_: IllegalArgumentException) {
        throw GradleException("APP_CONFIG_KEYS_BASE64 contains malformed properties data.")
    }
}

val bundledConfigApiKeys = sortedMapOf<String, MutableMap<String, String>>()
configApiKeyProperties.forEach { (rawName, rawValue) ->
    val parts = rawName.toString().split('.', limit = 2)
    if (parts.size != 2 || !parts[0].matches(Regex("[a-z][a-z0-9_]*")) ||
        !parts[1].matches(Regex("[A-Z][A-Z0-9_]*"))
    ) {
        throw GradleException(
            "Configuration API-key names must use <raw_resource_name>.<PLACEHOLDER_NAME>."
        )
    }
    val value = rawValue.toString()
    if (value.isEmpty()) {
        throw GradleException("Configuration API-key values must not be empty.")
    }
    bundledConfigApiKeys.getOrPut(parts[0]) { sortedMapOf() }[parts[1]] = value
}
val bundledConfigApiKeysJson = bundledConfigApiKeys.entries.joinToString(
    prefix = "{",
    postfix = "}"
) { (configurationName, values) ->
    val encodedValues = values.entries.joinToString(prefix = "{", postfix = "}") { (name, value) ->
        "${jsonString(name)}:${jsonString(value)}"
    }
    "${jsonString(configurationName)}:$encodedValues"
}
val bundledConfigApiKeysBase64 = Base64.getEncoder().encodeToString(
    bundledConfigApiKeysJson.toByteArray(StandardCharsets.UTF_8)
)

val releaseKeystorePath = environmentValue("ANDROID_KEYSTORE_PATH")
val releaseStorePassword = environmentValue("ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = environmentValue("ANDROID_KEY_ALIAS")
val releaseKeyPassword = environmentValue("ANDROID_KEY_PASSWORD")
val releaseSigningValues = listOf(
    releaseKeystorePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
)
val suppliedSigningValueCount = releaseSigningValues.count { it != null }
if (suppliedSigningValueCount in 1 until releaseSigningValues.size) {
    throw GradleException(
        "Release signing is only partially configured. Set all ANDROID_KEYSTORE_PATH, " +
            "ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS, and ANDROID_KEY_PASSWORD values."
    )
}
val releaseSigningConfigured = suppliedSigningValueCount == releaseSigningValues.size

android {
    namespace = "it.sapienza.androidratio.appratio"
    compileSdk = 36

    defaultConfig {
        applicationId = "it.sapienza.androidratio.appratio"
        minSdk = 27
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        buildConfigField("String", "CONFIG_API_KEYS_BASE64", "\"$bundledConfigApiKeysBase64\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { test ->
                val unitTestTempDirectory = layout.buildDirectory
                    .dir("tmp/unit-tests")
                    .get()
                    .asFile
                test.doFirst { unitTestTempDirectory.mkdirs() }
                test.workingDir = project.projectDir
                test.systemProperty("java.io.tmpdir", unitTestTempDirectory.absolutePath)
                test.jvmArgs(
                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                    "--add-opens=java.base/java.util=ALL-UNNAMED",
                    "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
                    "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
                    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED"
                )
                test.systemProperty(
                    "recordScreenshots",
                    project.findProperty("recordScreenshots") ?: "false"
                )
            }
        }
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(checkNotNull(releaseKeystorePath))
                storePassword = checkNotNull(releaseStorePassword)
                keyAlias = checkNotNull(releaseKeyAlias)
                keyPassword = checkNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            // Package the canonical repository schema without maintaining a second copy.
            assets.directories.add(rootProject.file("schema").absolutePath)
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.8")
    implementation("androidx.compose.foundation:foundation:1.6.8")
    implementation("androidx.compose.material3:material3:1.2.1")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.22.2")
    implementation("com.networknt:json-schema-validator:1.0.87")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("androidx.test:core-ktx:1.7.0")
    testImplementation("androidx.compose.ui:ui-test-junit4:1.6.8")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.8")
}

val configToValidate = providers.gradleProperty("config")
val runningConfigValidation = gradle.startParameter.taskNames.any {
    it.substringAfterLast(':') == "validateConfig"
}

tasks.withType<Test>().configureEach {
    if (runningConfigValidation) {
        filter {
            includeTestsMatching("it.sapienza.androidratio.appratio.ConfigSchemaValidationTest")
            includeTestsMatching("it.sapienza.androidratio.appratio.ConfigurationValidatorTest")
        }
    }
    if (configToValidate.isPresent) {
        val requestedConfig = rootProject.file(configToValidate.get())
        systemProperty("jsonApp.config", requestedConfig.absolutePath)
        inputs.file(requestedConfig).withPathSensitivity(PathSensitivity.RELATIVE)
    }
}

tasks.register("validateConfig") {
    group = "verification"
    description = "Validates bundled v3 configurations and the JSON file supplied with -Pconfig=<path>."
    dependsOn("testDebugUnitTest")

    doFirst {
        if (!configToValidate.isPresent) {
            throw GradleException(
                "Missing configuration path. Use ./gradlew :app:validateConfig -Pconfig=path/to/app.json"
            )
        }
        val requestedConfig = rootProject.file(configToValidate.get())
        if (!requestedConfig.isFile) {
            throw GradleException("Configuration file does not exist: $requestedConfig")
        }
    }
}
