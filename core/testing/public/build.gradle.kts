plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.coroutines)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.essenty.lifecycle)
        }
        androidMain.dependencies { implementation(kotlin("test-junit")) }
        jvmMain.dependencies { implementation(kotlin("test-junit")) }
    }
}
