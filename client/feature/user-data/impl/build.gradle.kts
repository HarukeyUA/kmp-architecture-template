plugins { alias(libs.plugins.convention.kmp.feature.impl) }

kotlin {
    sourceSets {
        commonMain.dependencies { implementation(project(":client:core:local-storage:public")) }
    }
}
