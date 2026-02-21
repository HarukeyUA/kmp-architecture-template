plugins {
    alias(libs.plugins.example.kmp.library)
    alias(libs.plugins.example.compose)
    alias(libs.plugins.serialization)
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
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        androidMain.dependencies { implementation(libs.kotlinx.coroutines.android) }
    }
}
