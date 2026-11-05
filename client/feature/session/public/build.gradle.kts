plugins {
    alias(libs.plugins.convention.kmp.feature.public)
    alias(libs.plugins.convention.arrow)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.arrow.core)
            api(libs.kermit)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.assertk)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
