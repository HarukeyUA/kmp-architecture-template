plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.arrow)
}

// CallFailure carries the shared ApiError (Declared/Ambient) so it rides the AppError pipeline
// (ADR-0011).
kotlin { sourceSets { commonMain.dependencies { api(project(":shared:common")) } } }
