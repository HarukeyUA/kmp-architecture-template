plugins { alias(libs.plugins.convention.kmp.compose.feature.public) }

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:navigation:public"))
            api(project(":feature:home:public"))
            api(project(":feature:search:public"))
            api(project(":feature:profile:public"))
        }
    }
}
