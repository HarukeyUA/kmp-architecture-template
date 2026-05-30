plugins { alias(libs.plugins.convention.kmp.compose.feature.public) }

// AuthRepository returns Either<AppError, Unit>; AppError comes transitively via :core:component.
kotlin { sourceSets { commonMain.dependencies { api(libs.arrow.core) } } }
