plugins {
    alias(libs.plugins.convention.kmp.feature.public)
    alias(libs.plugins.convention.ktor)
    alias(libs.plugins.convention.arrow)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":client:core:error:public"))
            // `sessionBearer` reads/refreshes the stored token pair through the secure store and
            // speaks the shared auth wire (refresh endpoint + DTOs).
            api(project(":client:core:secure-storage:public"))
            implementation(project(":shared:auth"))
            // Typed `HttpClient.call(Endpoint, …)` builds the request from a shared `@Resource`.
            api(libs.ktor.client.resources)
        }
    }
}
