package org.example.project

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.example.project.core.navigation.StackComponent
import org.example.project.feature.auth.LoginComponent
import org.example.project.feature.main.MainComponent
import org.example.project.feature.user.data.UserRepository

interface RootComponent : StackComponent<Any, RootComponent.Child> {
    sealed class Child {
        data object Splash : Child()

        data class Login(val component: LoginComponent) : Child()

        data class Main(val component: MainComponent) : Child()
    }

    fun interface Factory {
        fun create(componentContext: ComponentContext): RootComponent
    }
}

@AssistedInject
class DefaultRootComponent(
    @Assisted componentContext: ComponentContext,
    private val loginComponentFactory: LoginComponent.Factory,
    private val mainComponentFactory: MainComponent.Factory,
    private val userRepository: UserRepository,
) : RootComponent, ComponentContext by componentContext {
    private val coroutineScope = coroutineScope()

    private val navigation = StackNavigation<Config>()

    private val _stack =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            handleBackButton = false,
            initialStack = { listOf(Config.Splash) },
            childFactory = ::child,
        )
    override val stack: Value<ChildStack<*, RootComponent.Child>> = _stack

    init {
        determineStartDestination()
    }

    private fun determineStartDestination() {
        coroutineScope.launch {
            val isLoggedIn = userRepository.isLoggedIn.first()
            navigation.replaceAll(if (isLoggedIn) Config.Main else Config.Login)
        }
    }

    override fun onBackClick() {
        navigation.pop()
    }

    private fun child(config: Config, componentContext: ComponentContext): RootComponent.Child =
        when (config) {
            Config.Splash -> RootComponent.Child.Splash
            Config.Login ->
                RootComponent.Child.Login(
                    loginComponentFactory.create(
                        componentContext = componentContext,
                        onLoginSuccess = ::navigateToMain,
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
        override fun create(componentContext: ComponentContext): DefaultRootComponent
    }
}
