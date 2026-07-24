import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.metro)
    alias(libs.plugins.convention.spotless)
    alias(libs.plugins.convention.detekt)
}

/**
 * The build machine's site-local IPv4, or null when off-network. Used when DEV_SERVER_HOST does not
 * explicitly select the host shared with `scripts/dev-stack.sh --lan`. A ValueSource keeps the
 * automatic lookup configuration-cache compatible: a changed DHCP lease invalidates the cache entry
 * instead of being served stale.
 */
abstract class LanIpValueSource : ValueSource<String, ValueSourceParameters.None> {
    override fun obtain(): String? =
        runCatching {
                NetworkInterface.getNetworkInterfaces()
                    .asSequence()
                    .filter { runCatching { it.isUp && !it.isLoopback }.getOrDefault(false) }
                    // Physical NICs first: en* (macOS), wlan*/eth* (Linux) — keeps VPN tunnels and
                    // container bridges from winning when several site-local addresses exist.
                    .sortedBy { nic ->
                        when {
                            nic.name.startsWith("en") -> 0
                            nic.name.startsWith("wlan") -> 1
                            nic.name.startsWith("eth") -> 2
                            else -> 3
                        }
                    }
                    .flatMap { it.inetAddresses.asSequence() }
                    .filterIsInstance<Inet4Address>()
                    .firstOrNull { it.isSiteLocalAddress }
                    ?.hostAddress
            }
            .getOrNull()
}

fun gitCommitCount(): Int {
    val gitOutput = providers.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
        isIgnoreExitValue = true
    }

    return gitOutput.standardOutput.asText.map { it.trim().toIntOrNull() ?: 1 }.getOrElse(1)
}

fun versionNameFromFile(): String {
    val versionFile = rootProject.layout.projectDirectory.file("version.properties")

    return providers
        .fileContents(versionFile)
        .asText
        .map { content ->
            val properties = Properties().apply { load(content.reader()) }
            properties.getProperty("versionName")?.trim()
                ?: error("Property 'versionName' not found in ${versionFile.asFile.absolutePath}")
        }
        .get()
}

android {
    namespace = "com.rainy.myapplication"
    compileSdk { version = release(36) }

    defaultConfig {
        applicationId = "com.rainy.myapplication"
        minSdk = 26
        targetSdk = 36
        versionCode = gitCommitCount()
        versionName = versionNameFromFile()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // `environment` separates a personal "prod" install from a "dev" install that can sit on the
    // same device without sharing data — Android keys app-private storage off `applicationId`, so
    // a distinct id is all that's needed. The dev launcher label gains a " Dev" suffix via the
    // flavor's strings.xml override, and only the dev flavor allows cleartext HTTP (see
    // src/dev/AndroidManifest.xml).
    flavorDimensions += "environment"
    productFlavors {
        create("prod") {
            dimension = "environment"
            // Prod never talks to a local server; the field exists so main-source code compiles.
            buildConfigField("String", "DEV_SERVER_HOST", "\"\"")
        }
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            // DEV_SERVER_HOST is the deterministic escape hatch for multi-NIC/VPN machines and is
            // also consumed by dev-stack.sh (--lan) and the iOS xcconfig generator. Automatic
            // detection is retained for the zero-config path; empty/off-network falls back to
            // 10.0.2.2 at graph creation.
            val explicitHost =
                providers.environmentVariable("DEV_SERVER_HOST").orNull?.trim()?.takeIf {
                    it.isNotEmpty()
                }
            val devServerHost =
                explicitHost ?: providers.of(LanIpValueSource::class) {}.getOrElse("")
            require(devServerHost.isEmpty() || devServerHost.matches(Regex("[A-Za-z0-9.-]+"))) {
                "DEV_SERVER_HOST must be a hostname or IPv4 address without a scheme or port"
            }
            buildConfigField("String", "DEV_SERVER_HOST", "\"$devServerHost\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        // `BuildConfig.FLAVOR` / `DEV_SERVER_HOST` drive the injected Environment + dev host.
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)

    implementation(project(":client:core:buildinfo:public"))
    implementation(project(":client:composeApp"))
}
