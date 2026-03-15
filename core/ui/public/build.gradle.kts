plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.compose)
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
