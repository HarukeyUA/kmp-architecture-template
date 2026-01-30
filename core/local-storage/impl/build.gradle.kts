plugins { alias(libs.plugins.example.kmp.feature.impl) }

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json.okio)
            implementation(libs.androidx.datastore)
            implementation(libs.datastore.preferences)
        }
    }
}
