plugins {
    alias(libs.plugins.example.kmp.library)
    alias(libs.plugins.example.compose)
    alias(libs.plugins.example.serialization)
    alias(libs.plugins.example.coroutines)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.decompose)
            api(libs.essenty.lifecycle)
            api(libs.essenty.statekeeper)
            api(libs.essenty.backhandler)
            implementation(libs.molecule)
            implementation(libs.essenty.lifecycle.coroutines)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
    }
}
