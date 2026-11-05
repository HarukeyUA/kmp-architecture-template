package org.example.project.feature.main

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.MoleculeComponent
import org.example.project.core.ui.navigation.ScreenChild
import org.example.project.core.ui.navigation.asChild
import org.example.project.feature.settings.SettingsComponent
import org.example.project.feature.settings.SettingsScreen
import org.example.project.feature.tasks.TasksComponent
import org.example.project.feature.tasks.TasksScreen

@AssistedInject
class DefaultMainComponent(
    @Assisted componentContext: AppComponentContext,
    private val tasksComponentFactory: TasksComponent.Factory,
    private val settingsComponentFactory: SettingsComponent.Factory,
    private val tasksScreen: TasksScreen,
    private val settingsScreen: SettingsScreen,
) : MainComponent, MoleculeComponent<MainComponent.State>(componentContext) {

    private val navigation = StackNavigation<MainComponent.Tab>()

    override val stack: Value<ChildStack<MainComponent.Tab, ScreenChild>> =
        childStack(
            source = navigation,
            serializer = MainComponent.Tab.serializer(),
            initialConfiguration = MainComponent.Tab.Tasks,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    override fun onBackClick() {
        navigation.pop()
    }

    @Composable
    override fun produceState(): MainComponent.State =
        MainComponent.State(
            eventSink = { event ->
                when (event) {
                    is MainComponent.Event.TabClicked -> navigation.bringToFront(event.tab)
                }
            },
        )

    private fun createChild(
        tab: MainComponent.Tab,
        componentContext: AppComponentContext,
    ): ScreenChild =
        when (tab) {
            MainComponent.Tab.Tasks ->
                tasksComponentFactory.create(componentContext).asChild(tasksScreen::Content)
            MainComponent.Tab.Settings ->
                settingsComponentFactory.create(componentContext).asChild(settingsScreen::Content)
        }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : MainComponent.Factory {
        override fun create(componentContext: AppComponentContext): DefaultMainComponent
    }
}
