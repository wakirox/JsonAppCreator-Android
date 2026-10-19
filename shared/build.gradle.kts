// The engine of Appkin without its UI: the model, parsing and validation, the runtime state and use cases, and the
// editor and AI logic. Android uses it from :app; an iPhone app would link the same code as a framework.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    android {
        namespace = "it.sapienza.appinterpreter.shared"
        compileSdk = 37
        minSdk = 33
        withHostTestBuilder {}
    }
    // Declared so common code stays iOS-ready; the framework can only be linked on a Mac.
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "AppkinShared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
        }
    }
}
