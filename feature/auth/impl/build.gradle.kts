plugins {
    alias(libs.plugins.example.kmp.compose.feature.impl)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":feature:user-data:public"))
        }
    }
}