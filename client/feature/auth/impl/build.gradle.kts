plugins {
    alias(libs.plugins.convention.kmp.compose.feature.impl)
    alias(libs.plugins.convention.screenshot.testing)
}

kotlin {
    sourceSets {
        commonMain.dependencies { implementation(project(":client:feature:user-data:public")) }
        commonTest.dependencies { implementation(project(":client:feature:user-data:testing")) }
    }
}
