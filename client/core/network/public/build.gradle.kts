plugins {
    alias(libs.plugins.convention.kmp.feature.public)
    alias(libs.plugins.convention.ktor)
    alias(libs.plugins.convention.arrow)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":client:core:error:public"))
            // Typed `HttpClient.call(Endpoint, …)` builds the request from a shared `@Resource`.
            api(libs.ktor.client.resources)
        }
    }
}
