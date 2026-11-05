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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.lifecycleAwareScope
import org.example.project.core.component.snackbar.SnackbarHostState
import org.example.project.core.component.snackbar.snackbarHost
import org.example.project.core.navigation.StackComponent
import org.example.project.feature.auth.LoginComponent
import org.example.project.feature.home.HomeComponent
import org.example.project.feature.session.SessionState
import org.example.project.feature.session.SessionStatus
import org.example.project.feature.splash.presentation.SplashComponent

interface RootComponent : StackComponent<Any, RootComponent.Child> {

    val snackbarHostState: SnackbarHostState

    sealed class Child {
        data class Splash(val component: SplashComponent) : Child()

        data class Login(val component: LoginComponent) : Child()

        data class Home(val component: HomeComponent) : Child()
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
    private val homeComponentFactory: HomeComponent.Factory,
    private val sessionStatus: SessionStatus,
) : RootComponent, AppComponentContext by componentContext {
    private val scope = lifecycleAwareScope()

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
        observeSignedInState()
    }

    private fun observeSignedInState() {
        scope.launch {
            sessionStatus
                .observe()
                .map { it is SessionState.Bound }
                .distinctUntilChanged()
                .drop(1)
                .collect { signedIn ->
                    if (!signedIn) {
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
                        onNavigateToHome = ::navigateToHome,
                        onNavigateToLogin = ::navigateToLogin,
                    ),
                )

            Config.Login ->
                RootComponent.Child.Login(
                    loginComponentFactory.create(
                        componentContext = componentContext,
                        onAuthenticated = ::navigateToHome,
                    ),
                )

            Config.Home ->
                RootComponent.Child.Home(
                    homeComponentFactory.create(
                        componentContext = componentContext,
                        onLogout = ::navigateToLogin,
                    ),
                )
        }

    private fun navigateToHome() {
        navigation.replaceAll(Config.Home)
    }

    private fun navigateToLogin() {
        navigation.replaceAll(Config.Login)
    }

    @Serializable
    private sealed interface Config {
        @Serializable data object Splash : Config

        @Serializable data object Login : Config

        @Serializable data object Home : Config
    }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : RootComponent.Factory {
        override fun create(componentContext: AppComponentContext): DefaultRootComponent
    }
}
