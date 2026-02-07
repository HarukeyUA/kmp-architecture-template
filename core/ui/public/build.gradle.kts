plugins {
    alias(libs.plugins.example.kmp.library)
    alias(libs.plugins.example.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.compose.material3)
            api(libs.decompose.compose)
            api(libs.decompose.compose.experimental)
            api(project(":core:navigation:public"))
        }
    }
}
