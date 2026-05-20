// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.metro) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.convention.spotless)
}
