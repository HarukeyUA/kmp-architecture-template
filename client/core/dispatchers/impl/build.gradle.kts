import org.example.project.siblingPublicModule

plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.coroutines)
    alias(libs.plugins.convention.metro)
}

kotlin { sourceSets { commonMain.dependencies { api(project(siblingPublicModule())) } } }
