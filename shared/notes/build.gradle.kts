plugins { alias(libs.plugins.convention.shared.contract) }

kotlin {
    sourceSets {
        commonMain.dependencies { api(project(":shared:common")) }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.assertk)
        }
    }
}
