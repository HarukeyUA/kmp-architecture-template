plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.compose)
    alias(libs.plugins.convention.serialization)
    alias(libs.plugins.convention.coroutines)
    alias(libs.plugins.convention.arrow)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":client:core:error:public"))
            api(libs.decompose)
            api(libs.essenty.lifecycle)
            api(libs.essenty.statekeeper)
            api(libs.essenty.backhandler)
            api(libs.molecule)
            implementation(libs.essenty.lifecycle.coroutines)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.assertk)
        }
    }
}
