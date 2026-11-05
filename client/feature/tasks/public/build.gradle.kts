plugins {
    alias(libs.plugins.convention.kmp.compose.feature.public)
    alias(libs.plugins.convention.arrow)
}

// TasksRepository returns the operations' Declared errors, which come from :shared:tasks.
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.arrow.core)
            api(project(":shared:tasks"))
            implementation(libs.compose.foundation)
        }
    }
}
