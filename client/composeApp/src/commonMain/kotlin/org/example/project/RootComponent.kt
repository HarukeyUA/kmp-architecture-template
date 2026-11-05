package org.example.project

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import org.example.project.core.component.MoleculeComponent
import org.example.project.core.component.StatefulComponent
import org.example.project.core.component.UiState
import org.example.project.core.component.snackbar.SnackbarHostState
import org.example.project.core.component.snackbar.snackbarHost
import org.example.project.core.local.storage.SettingsLocalDataSource
import org.example.project.core.local.storage.model.SettingsLocalModel
import org.example.project.core.local.storage.model.ThemeMode
import org.example.project.core.navigation.StackComponent
import org.example.project.feature.auth.LoginComponent
import org.example.project.feature.main.MainComponent
import org.example.project.feature.session.SessionEndWiper
import org.example.project.feature.session.SessionState
import org.example.project.feature.session.SessionStatus
import org.example.project.feature.splash.presentation.SplashComponent

interface RootComponent :
    StackComponent<Any, RootComponent.Child>, StatefulComponent<RootComponent.State> {

    val snackbarHostState: SnackbarHostState

    data class State(val themeMode: ThemeMode = ThemeMode.System) : UiState

    sealed interface Child {
        data class Splash(val component: SplashComponent) : Child

        data class Login(val component: LoginComponent) : Child

        data class Main(val component: MainComponent) : Child
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
    private val sessionStatus: SessionStatus,
    private val sessionEndWiper: SessionEndWiper,
    private val settings: SettingsLocalDataSource,
) : RootComponent, MoleculeComponent<RootComponent.State>(componentContext) {

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
        scope.launch { sessionEndWiper.watch() }
    }

    @Composable
    override fun produceState(): RootComponent.State {
        val current by remember {
            settings.settingsFlow
        }
            .collectAsStateWithLifecycle(initialValue = SettingsLocalModel())
        return RootComponent.State(themeMode = current.themeMode)
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
                        onNavigateToMain = ::navigateToMain,
                        onNavigateToLogin = ::navigateToLogin,
                    ),
                )

            Config.Login ->
                RootComponent.Child.Login(
                    loginComponentFactory.create(
                        componentContext = componentContext,
                        onAuthenticated = ::navigateToMain,
                    ),
                )

            Config.Main -> RootComponent.Child.Main(mainComponentFactory.create(componentContext))
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
