import org.example.project.siblingPublicModule

plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.coroutines)
    alias(libs.plugins.convention.serialization)
    alias(libs.plugins.convention.metro)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(siblingPublicModule()))
            implementation(libs.kotlinx.serialization.json.okio)
            implementation(libs.androidx.datastore)
            implementation(libs.datastore.preferences)
        }
    }
}
