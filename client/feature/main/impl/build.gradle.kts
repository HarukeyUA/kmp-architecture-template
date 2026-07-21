plugins {
    alias(libs.plugins.convention.kmp.compose.feature.impl)
    alias(libs.plugins.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":client:feature:home:public"))
            implementation(project(":client:feature:notes:public"))
            implementation(project(":client:feature:profile:public"))
            implementation(libs.decompose.compose.experimental)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
