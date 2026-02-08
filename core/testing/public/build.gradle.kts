plugins { alias(libs.plugins.example.kmp.feature.public) }

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.essenty.lifecycle)
        }
        jvmMain.dependencies { implementation(kotlin("test-junit")) }
    }
}
