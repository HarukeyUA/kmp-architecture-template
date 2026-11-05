plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.coroutines)
    alias(libs.plugins.convention.arrow)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.arrow.core)
            implementation(libs.room.runtime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.assertk)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
