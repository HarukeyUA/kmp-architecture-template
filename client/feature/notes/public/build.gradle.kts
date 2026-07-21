plugins { alias(libs.plugins.convention.kmp.compose.feature.public) }

// NotesRepository returns Either<CallFailure<NotesCreateError>, T> and State carries the declared
// NotesQuotaExceeded: CallFailure comes transitively via :core:component (:core:error); the notes
// declared lens/variant come from :shared:notes.
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.arrow.core)
            api(project(":shared:notes"))
        }
    }
}
