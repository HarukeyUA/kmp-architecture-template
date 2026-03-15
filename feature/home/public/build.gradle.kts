plugins { alias(libs.plugins.convention.kmp.compose.feature.public) }

kotlin { sourceSets { commonMain.dependencies { api(project(":core:navigation:public")) } } }
