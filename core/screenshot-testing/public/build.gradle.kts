plugins {
    alias(libs.plugins.example.kmp.compose.feature.public)
    alias(libs.plugins.roborazzi)
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.roborazzi)
            implementation(libs.roborazzi.compose)
            implementation(libs.robolectric)
            implementation(libs.junit)
            implementation(libs.compose.ui.test.junit4)
            implementation(kotlin("test-junit"))
            implementation(project(":core:ui:public"))
        }
    }
}
