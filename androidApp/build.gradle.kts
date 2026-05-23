plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.metro)
    alias(libs.plugins.convention.spotless)
    alias(libs.plugins.convention.detekt)
}

fun gitCommitCount(): Int {
    val output = providers.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
        isIgnoreExitValue = true
    }
    return if (output.result.get().exitValue == 0) {
        output.standardOutput.asText.get().trim().toIntOrNull() ?: 1
    } else {
        1
    }
}

android {
    namespace = "com.rainy.myapplication"
    compileSdk { version = release(36) }

    defaultConfig {
        applicationId = "com.rainy.myapplication"
        minSdk = 26
        targetSdk = 36
        versionCode = gitCommitCount()
        versionName = "1.0"

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
