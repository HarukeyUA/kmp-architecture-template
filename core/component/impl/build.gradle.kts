plugins {
    alias(libs.plugins.example.kmp.library)
    alias(libs.plugins.example.compose)
    alias(libs.plugins.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:component:public"))
            implementation(libs.molecule)
            implementation(libs.decompose)
            implementation(libs.essenty.lifecycle)
            implementation(libs.essenty.statekeeper)
            implementation(libs.essenty.lifecycle.coroutines)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }
    }
}
