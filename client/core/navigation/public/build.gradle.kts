plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":client:core:component:public"))
            api(libs.decompose)
            api(libs.essenty.statekeeper)
        }
    }
}
