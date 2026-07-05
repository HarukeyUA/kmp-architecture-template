package org.example.project.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import arrow.core.Either
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.launch
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.MoleculeComponent
import org.example.project.core.error.AppError
import org.example.project.core.error.CallFailure
import org.example.project.shared.auth.AuthLoginError
import org.example.project.shared.auth.AuthSignupError
import org.example.project.shared.auth.EmailTaken
import org.example.project.shared.auth.InvalidCredentials
import org.example.project.shared.common.ApiError

@AssistedInject
class DefaultLoginComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted private val onAuthenticated: () -> Unit,
    private val authRepository: AuthRepository,
) :
    LoginComponent,
    MoleculeComponent<LoginComponent.State, LoginComponent.Event>(componentContext) {

    @Composable
    override fun produceState(): LoginComponent.State {
        var email by rememberSaveable { mutableStateOf("") }
        var password by rememberSaveable { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<AppError?>(null) }
        var formError by remember { mutableStateOf<LoginComponent.FormError?>(null) }

        // Declared failures ([declaredForm], an exhaustive `when` over the operation's error lens)
        // render inline; everything else (Ambient/Transport) rides the generic renderer pipeline.
        fun <E : ApiError> submit(
            action: suspend (String, String) -> Either<CallFailure<E>, Unit>,
            declaredForm: (E) -> LoginComponent.FormError,
        ) {
            if (isSubmitting) return
            isSubmitting = true
            error = null
            formError = null
            scope.launch {
                action(email, password)
                    .fold(
                        ifLeft = { failure ->
                            when (failure) {
                                is CallFailure.Declared -> formError = declaredForm(failure.error)
                                else -> error = failure
                            }
                            isSubmitting = false
                        },
                        // On success the issued Session is already in secure storage (which flips
                        // the observed logged-in state); this callback drives the immediate nav.
                        ifRight = { onAuthenticated() },
                    )
            }
        }

        CollectEvents { event ->
            when (event) {
                is LoginComponent.Event.EmailChanged -> {
                    email = event.value
                    error = null
                    formError = null
                }
                is LoginComponent.Event.PasswordChanged -> {
                    password = event.value
                    error = null
                    formError = null
                }
                LoginComponent.Event.LoginClicked ->
                    submit(authRepository::login) { declared: AuthLoginError ->
                        when (declared) {
                            InvalidCredentials -> LoginComponent.FormError.InvalidCredentials
                        }
                    }
                LoginComponent.Event.SignupClicked ->
                    submit(authRepository::signup) { declared: AuthSignupError ->
                        when (declared) {
                            EmailTaken -> LoginComponent.FormError.EmailTaken
                        }
                    }
            }
        }

        return LoginComponent.State(
            email = email,
            password = password,
            isSubmitting = isSubmitting,
            error = error,
            formError = formError,
        )
    }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : LoginComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            onAuthenticated: () -> Unit,
        ): DefaultLoginComponent
    }
}
