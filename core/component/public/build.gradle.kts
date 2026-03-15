plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.compose)
    alias(libs.plugins.convention.serialization)
    alias(libs.plugins.convention.coroutines)
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
