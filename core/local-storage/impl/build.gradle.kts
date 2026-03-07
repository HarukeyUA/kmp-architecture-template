import org.example.project.siblingPublicModule

plugins {
    alias(libs.plugins.example.kmp.library)
    alias(libs.plugins.example.coroutines)
    alias(libs.plugins.example.serialization)
    alias(libs.plugins.example.metro)
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
