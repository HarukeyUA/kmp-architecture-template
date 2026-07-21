plugins {
    alias(libs.plugins.convention.kmp.compose.feature.impl)
    alias(libs.plugins.convention.screenshot.testing)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":client:core:network:public"))
            implementation(project(":client:core:secure-storage:public"))
            implementation(project(":shared:auth"))
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.resources)
        }
        commonTest.dependencies {
            // The MockEngine test reconstructs the HttpClient (ContentNegotiation + Auth bearer) to
            // exercise AuthRepository's request building and the 401 → clear-session interceptor.
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
    }
}
