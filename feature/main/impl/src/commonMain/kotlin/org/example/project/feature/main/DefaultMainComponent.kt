package org.example.project.feature.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import kotlinx.serialization.Serializable
import org.example.project.feature.home.HomeComponent
import org.example.project.feature.profile.ProfileComponent
import org.example.project.feature.search.SearchComponent

@AssistedInject
class DefaultMainComponent(
    @Assisted componentContext: ComponentContext,
    @Assisted private val onLogout: () -> Unit,
    private val homeComponentFactory: HomeComponent.Factory,
    private val searchComponentFactory: SearchComponent.Factory,
    private val profileComponentFactory: ProfileComponent.Factory,
) : MainComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    private val _stack: Value<ChildStack<Config, MainComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Home,
            childFactory = ::createChild,
        )

    override val stack: Value<ChildStack<*, MainComponent.Child>> = _stack

    override fun onEvent(event: MainComponent.Event) {
        when (event) {
            MainComponent.Event.HomeTabClick -> navigation.bringToFront(Config.Home)
            MainComponent.Event.ProfileTabClick -> navigation.bringToFront(Config.Profile)
            MainComponent.Event.SearchTabClick -> navigation.bringToFront(Config.Search)
        }
    }

    private fun createChild(
        config: Config,
        componentContext: ComponentContext,
    ): MainComponent.Child =
        when (config) {
            Config.Home ->
                MainComponent.Child.Home(
                    homeComponentFactory.create(componentContext = componentContext)
                )

            Config.Search ->
                MainComponent.Child.Search(
                    searchComponentFactory.create(componentContext = componentContext)
                )

            Config.Profile ->
                MainComponent.Child.Profile(
                    profileComponentFactory.create(
                        componentContext = componentContext,
                        onLogout = onLogout,
                    )
                )
        }

    @Serializable
    private sealed interface Config {
        @Serializable data object Home : Config

        @Serializable data object Search : Config

        @Serializable data object Profile : Config
    }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : MainComponent.Factory {
        override fun create(
            componentContext: ComponentContext,
            onLogout: () -> Unit,
        ): DefaultMainComponent
    }
}
