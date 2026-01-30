plugins {
    alias(libs.plugins.example.kmp.compose.feature.impl)
    alias(libs.plugins.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":feature:home:public"))
            implementation(project(":feature:search:public"))
            implementation(project(":feature:profile:public"))
            implementation(libs.decompose.compose.experimental)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
