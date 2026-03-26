import org.example.project.siblingPublicModule

plugins {
    alias(libs.plugins.convention.kmp.feature.impl)
    alias(libs.plugins.convention.ktor)
}

kotlin { sourceSets { commonMain.dependencies { api(project(siblingPublicModule())) } } }
