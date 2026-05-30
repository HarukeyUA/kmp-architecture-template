plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.compose)
    alias(libs.plugins.convention.compose.resources)
    alias(libs.plugins.convention.metro)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.compose.material3)
            api(libs.compose.resources)
            api(libs.decompose.compose)
            api(libs.decompose.compose.experimental)
            api(project(":core:navigation:public"))
            api(project(":core:error:public"))
        }
    }
}
