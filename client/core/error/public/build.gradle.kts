plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.arrow)
}

// NetworkError.Api wraps the shared ApiError so it rides the existing AppError pipeline (ADR-0005).
kotlin { sourceSets { commonMain.dependencies { api(project(":shared:common")) } } }
