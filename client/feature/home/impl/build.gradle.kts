plugins {
    alias(libs.plugins.convention.kmp.compose.feature.impl)
    alias(libs.plugins.convention.screenshot.testing)
}

kotlin {
    sourceSets {
        commonMain.dependencies { implementation(project(":client:feature:auth:public")) }
    }
}
