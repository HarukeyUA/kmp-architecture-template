package org.example.project.feature.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.router.pages.Pages
import com.arkivanov.decompose.router.pages.PagesNavigation
import com.arkivanov.decompose.router.pages.childPages
import com.arkivanov.decompose.router.pages.select
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

    private val navigation = PagesNavigation<PageConfig>()

    override val pages: Value<ChildPages<Any, MainComponent.Child>> =
        childPages(
            source = navigation,
            serializer = PageConfig.serializer(),
            initialPages = {
                Pages(
                    items = listOf(PageConfig.Home, PageConfig.Search, PageConfig.Profile),
                    selectedIndex = 0,
                )
            },
            childFactory = ::createChild,
        )

    override fun selectPage(index: Int) {
        navigation.select(index = index)
    }

    private fun createChild(
        config: PageConfig,
        componentContext: ComponentContext,
    ): MainComponent.Child =
        when (config) {
            PageConfig.Home ->
                MainComponent.Child.Home(
                    homeComponentFactory.create(componentContext = componentContext)
                )

            PageConfig.Search ->
                MainComponent.Child.Search(
                    searchComponentFactory.create(componentContext = componentContext)
                )

            PageConfig.Profile ->
                MainComponent.Child.Profile(
                    profileComponentFactory.create(
                        componentContext = componentContext,
                        onLogout = onLogout,
                    )
                )
        }

    @Serializable
    private sealed interface PageConfig {
        @Serializable data object Home : PageConfig

        @Serializable data object Search : PageConfig

        @Serializable data object Profile : PageConfig
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
