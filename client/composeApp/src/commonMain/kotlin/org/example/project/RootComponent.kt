package org.example.project

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.lifecycleAwareScope
import org.example.project.core.component.snackbar.SnackbarHostState
import org.example.project.core.component.snackbar.snackbarHost
import org.example.project.core.navigation.StackComponent
import org.example.project.feature.auth.LoginComponent
import org.example.project.feature.main.presentation.MainComponent
import org.example.project.feature.splash.presentation.SplashComponent
import org.example.project.feature.user.data.UserRepository

interface RootComponent : StackComponent<Any, RootComponent.Child> {

    val snackbarHostState: SnackbarHostState

    sealed class Child {
        data class Splash(val component: SplashComponent) : Child()

        data class Login(val component: LoginComponent) : Child()

        data class Main(val component: MainComponent) : Child()
    }

    fun interface Factory {
        fun create(componentContext: AppComponentContext): RootComponent
    }
}

@AssistedInject
class DefaultRootComponent(
    @Assisted componentContext: AppComponentContext,
    private val splashComponentFactory: SplashComponent.Factory,
    private val loginComponentFactory: LoginComponent.Factory,
    private val mainComponentFactory: MainComponent.Factory,
    userRepository: UserRepository,
) : RootComponent, AppComponentContext by componentContext {

    override val snackbarHostState = snackbarHost()

    private val navigation = StackNavigation<Config>()

    private val _stack =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            handleBackButton = true,
            initialStack = { listOf(Config.Splash) },
            childFactory = ::child,
        )
    override val stack: Value<ChildStack<*, RootComponent.Child>> = _stack

    init {
        // Bounce to Login whenever the session is lost while signed in — covers both an explicit
        // logout and the HttpClient's global 401 interceptor clearing a revoked session (ADR-0009).
        lifecycleAwareScope().launch {
            userRepository.isLoggedIn.collect { loggedIn ->
                if (!loggedIn && _stack.value.active.configuration is Config.Main) {
                    navigateToLogin()
                }
            }
        }
    }

    override fun onBackClick() {
        navigation.pop()
    }

    private fun child(config: Config, componentContext: AppComponentContext): RootComponent.Child =
        when (config) {
            Config.Splash ->
                RootComponent.Child.Splash(
                    splashComponentFactory.create(
                        componentContext = componentContext,
                        onNavigateToMain = ::navigateToMain,
                        onNavigateToLogin = ::navigateToLogin,
                    )
                )

            Config.Login ->
                RootComponent.Child.Login(
                    loginComponentFactory.create(
                        componentContext = componentContext,
                        onAuthenticated = ::navigateToMain,
                    )
                )

            is Config.Main ->
                RootComponent.Child.Main(
                    mainComponentFactory.create(
                        componentContext = componentContext,
                        onLogout = ::navigateToLogin,
                    )
                )
        }

    private fun navigateToMain() {
        navigation.replaceAll(Config.Main)
    }

    private fun navigateToLogin() {
        navigation.replaceAll(Config.Login)
    }

    @Serializable
    private sealed interface Config {
        @Serializable data object Splash : Config

        @Serializable data object Login : Config

        @Serializable data object Main : Config
    }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : RootComponent.Factory {
        override fun create(componentContext: AppComponentContext): DefaultRootComponent
    }
}
