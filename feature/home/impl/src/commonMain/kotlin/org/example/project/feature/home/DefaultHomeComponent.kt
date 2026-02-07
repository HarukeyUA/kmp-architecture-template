package org.example.project.feature.home

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import kotlinx.serialization.Serializable

@AssistedInject
class DefaultHomeComponent(
    @Assisted componentContext: ComponentContext,
    private val homeListComponentFactory: HomeListComponent.Factory,
    private val homeDetailComponentFactory: HomeDetailComponent.Factory,
) : HomeComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    private val _stack: Value<ChildStack<Config, HomeComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.List,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    override val stack: Value<ChildStack<Any, HomeComponent.Child>> = _stack

    override fun onBackClick() {
        navigation.pop()
    }

    private fun createChild(
        config: Config,
        componentContext: ComponentContext,
    ): HomeComponent.Child =
        when (config) {
            Config.List ->
                HomeComponent.Child.List(
                    homeListComponentFactory.create(
                        componentContext = componentContext,
                        onItemSelected = { id -> navigation.pushNew(Config.Detail(id)) },
                    )
                )

            is Config.Detail ->
                HomeComponent.Child.Detail(
                    homeDetailComponentFactory.create(
                        componentContext = componentContext,
                        itemId = config.itemId,
                        onBack = { navigation.pop() },
                    )
                )
        }

    @Serializable
    private sealed interface Config {
        @Serializable data object List : Config

        @Serializable data class Detail(val itemId: Int) : Config
    }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : HomeComponent.Factory {
        override fun create(componentContext: ComponentContext): DefaultHomeComponent
    }
}
