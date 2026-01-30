plugins {
    alias(libs.plugins.example.kmp.library)
    alias(libs.plugins.example.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.compose.material3)
        }
    }
}
