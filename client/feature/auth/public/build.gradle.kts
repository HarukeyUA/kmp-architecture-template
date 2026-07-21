plugins { alias(libs.plugins.convention.kmp.compose.feature.public) }

// AuthRepository returns Either<CallFailure<AuthLoginError/AuthSignupError>, Unit>: CallFailure
// comes transitively via :core:component (:core:error); the Declared lenses come from :shared:auth.
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.arrow.core)
            api(project(":shared:auth"))
        }
    }
}
