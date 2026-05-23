import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.metro)
    alias(libs.plugins.convention.spotless)
    alias(libs.plugins.convention.detekt)
}

fun gitCommitCount(): Int {
    val gitOutput = providers.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
        isIgnoreExitValue = true
    }

    return gitOutput.standardOutput.asText
        .map { it.trim().toIntOrNull() ?: 1 }
        .getOrElse(1)
}

fun versionNameFromFile(): String {
    val versionFile = rootProject.layout.projectDirectory.file("version.properties")

    return providers.fileContents(versionFile).asText.map { content ->
        val properties = Properties().apply {
            load(content.reader())
        }
        properties.getProperty("versionName")?.trim()
            ?: error("Property 'versionName' not found in ${versionFile.asFile.absolutePath}")
    }.get()
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
    buildFeatures { compose = true }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)

    implementation(project(":composeApp"))
}
