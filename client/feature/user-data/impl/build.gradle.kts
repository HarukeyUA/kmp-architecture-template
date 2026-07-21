plugins { alias(libs.plugins.convention.kmp.feature.impl) }

kotlin {
    sourceSets {
        commonMain.dependencies { implementation(project(":client:core:secure-storage:public")) }
    }
}
