plugins {
    alias(libs.plugins.convention.kmp.feature.impl)
    alias(libs.plugins.convention.ktor)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // The HttpClient pulls the bearer token from secure storage and clears it on 401.
            implementation(project(":client:core:secure-storage:public"))
            // Wire logging is gated on the injected Environment (DEV only).
            implementation(project(":client:core:buildinfo:public"))
            implementation(libs.ktor.client.resources)
            implementation(libs.kermit)
        }
    }
}
