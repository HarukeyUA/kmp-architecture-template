plugins {
    alias(libs.plugins.convention.kmp.feature.impl)
    alias(libs.plugins.convention.ktor)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // The HttpClient pulls the bearer token from secure storage and clears it on 401.
            implementation(project(":client:core:secure-storage:public"))
            implementation(libs.ktor.client.resources)
        }
    }
}
