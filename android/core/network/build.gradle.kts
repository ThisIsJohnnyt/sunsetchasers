import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

// Defaults to the emulator/LAN dev setup (10.0.2.2 is the emulator's alias
// for host localhost; the LAN IP below is for a physical device on the same
// network as the dev machine — see GETTING_STARTED.md). Override per-build
// via `backendBaseUrl=...` in android/local.properties (gitignored, so this
// stays per-machine) or `-PbackendBaseUrl=...` on the command line — e.g. to
// point a "vacation build" at a hosted backend without touching source.
val backendBaseUrl: String = (project.findProperty("backendBaseUrl") as String?)
    ?: localProperties.getProperty("backendBaseUrl")
    ?: "http://192.168.1.59:8080/"

android {
    namespace = "com.sunsetchasers.core.network"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        buildConfigField("String", "BASE_URL", "\"$backendBaseUrl\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:model"))

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
}
