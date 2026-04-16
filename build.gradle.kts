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
}

// Spotless on the root project formats `build.gradle.kts`, `settings.gradle.kts`, and everything
// under `build-logic/`. Leaf modules get Spotless via `convention.kmp.library` or by
// applying `convention.spotless` directly (`:androidApp`).
//
// We apply it imperatively instead of via the `plugins { alias(...) }` block because
// `build-logic:settings` is already on the root's classpath (the settings plugin
// `convention.impl-aggregator.settings` loads it at settings-evaluation time), and the plugins
// DSL refuses to re-resolve an id that is already on the buildscript classpath.
apply(plugin = "convention.spotless")
