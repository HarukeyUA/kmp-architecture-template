plugins {
    alias(libs.plugins.convention.kmp.compose.feature.impl)
    alias(libs.plugins.convention.screenshot.testing)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":client:core:database:public"))
            implementation(project(":client:core:datetime:public"))
            implementation(project(":client:core:network:public"))
            implementation(project(":client:feature:session:public"))
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.resources)
        }
        commonTest.dependencies {
            implementation(project(":client:core:database:testing"))
            implementation(project(":client:feature:session:testing"))
            implementation(project(":client:feature:tasks:testing"))
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
    }
}
