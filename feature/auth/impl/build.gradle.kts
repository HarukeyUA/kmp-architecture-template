plugins {
    alias(libs.plugins.example.kmp.compose.feature.impl)
    alias(libs.plugins.example.screenshot.testing)
}

kotlin {
    sourceSets {
        commonMain.dependencies { implementation(project(":feature:user-data:public")) }
        commonTest.dependencies { implementation(project(":feature:user-data:testing")) }
    }
}
