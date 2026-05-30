plugins { alias(libs.plugins.convention.kmp.library) }

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":client:core:component:public"))
            api(libs.decompose)
        }
    }
}
