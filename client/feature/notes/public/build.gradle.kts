plugins { alias(libs.plugins.convention.kmp.compose.feature.public) }

// NotesRepository returns Either<AppError, T>; AppError comes transitively via :core:component.
kotlin { sourceSets { commonMain.dependencies { api(libs.arrow.core) } } }
