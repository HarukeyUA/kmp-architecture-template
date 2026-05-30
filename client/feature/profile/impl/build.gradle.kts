plugins { alias(libs.plugins.convention.kmp.compose.feature.impl) }

kotlin {
    sourceSets {
        commonMain.dependencies { implementation(project(":client:feature:auth:public")) }
    }
}
