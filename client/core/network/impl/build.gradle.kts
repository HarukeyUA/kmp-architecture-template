plugins {
    alias(libs.plugins.convention.kmp.feature.impl)
    alias(libs.plugins.convention.ktor)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Wire logging is gated on the injected Environment (DEV only).
            implementation(project(":client:core:buildinfo:public"))
            implementation(libs.ktor.client.resources)
            implementation(libs.kermit)
        }
    }
}
