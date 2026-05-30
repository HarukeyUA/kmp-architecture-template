plugins { alias(libs.plugins.convention.kmp.library) }

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":client:feature:user-data:public"))
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
