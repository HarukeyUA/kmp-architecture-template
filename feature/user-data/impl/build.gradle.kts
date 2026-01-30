plugins {
    alias(libs.plugins.example.kmp.feature.impl)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:local-storage:public"))
        }
    }
}
