plugins { alias(libs.plugins.example.kmp.compose.feature.public) }

kotlin { sourceSets { commonMain.dependencies { api(project(":core:navigation:public")) } } }
