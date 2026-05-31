plugins { alias(libs.plugins.convention.kmp.compose.feature.impl) }

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":client:core:network:public"))
            implementation(project(":shared:notes"))
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.resources)
        }
        commonTest.dependencies {
            // The MockEngine test exercises NotesRepository's request building, the toModel
            // mapping,
            // and the typed NoteLimitReached round-trip through the client's seam Json.
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
    }
}
