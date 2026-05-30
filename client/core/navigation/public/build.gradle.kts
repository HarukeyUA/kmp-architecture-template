plugins { alias(libs.plugins.convention.kmp.library) }

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:component:public"))
            api(libs.decompose)
        }
    }
}
