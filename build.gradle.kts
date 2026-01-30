// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.metro) apply false
    alias(libs.plugins.spotless) apply false
}

allprojects {
    apply(plugin = rootProject.libs.plugins.spotless.get().pluginId)
    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            ktfmt(libs.ktfmt.get().version).kotlinlangStyle()
            target("src/**/*.kt", "build-logic/**/*.kt")
        }
        kotlinGradle {
            ktfmt(libs.ktfmt.get().version).kotlinlangStyle()
            target("*.kts")
        }
        format("xml") { target("src/**/*.xml") }
    }
}
