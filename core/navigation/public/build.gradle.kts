plugins { alias(libs.plugins.example.kmp.library) }

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:component:public"))
            api(libs.decompose)
        }
    }
}
