package org.example.project.feature.main.presentation

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
import org.example.project.core.component.snackbar.snackbarHost
import org.example.project.core.ui.navigation.ScreenChild
import org.example.project.core.ui.navigation.asChild
import org.example.project.feature.home.host.presentation.HomeComponent
import org.example.project.feature.home.host.presentation.HomeScreen
import org.example.project.feature.notes.NotesComponent
import org.example.project.feature.notes.NotesScreen
import org.example.project.feature.profile.presentation.ProfileComponent
import org.example.project.feature.profile.presentation.ProfileScreen

@AssistedInject
class DefaultMainComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted private val onLogout: () -> Unit,
    private val homeComponentFactory: HomeComponent.Factory,
    private val notesComponentFactory: NotesComponent.Factory,
    private val profileComponentFactory: ProfileComponent.Factory,
    private val homeScreen: HomeScreen,
    private val notesScreen: NotesScreen,
    private val profileScreen: ProfileScreen,
) : MainComponent, MoleculeComponent<MainComponent.State>(componentContext) {

    override val snackbarHostState = snackbarHost()

    private val navigation = StackNavigation<MainComponent.Tab>()

    private val _stack: Value<ChildStack<MainComponent.Tab, ScreenChild>> =
        childStack(
            source = navigation,
            serializer = MainComponent.Tab.serializer(),
            initialConfiguration = MainComponent.Tab.Home,
            childFactory = ::createChild,
        )

    override val stack: Value<ChildStack<MainComponent.Tab, ScreenChild>> = _stack

    override fun onBackClick() {
        navigation.pop()
    }

    @Composable
    override fun produceState(): MainComponent.State =
        MainComponent.State(
            eventSink = { event ->
                when (event) {
                    MainComponent.Event.HomeTabClick ->
                        navigation.bringToFront(MainComponent.Tab.Home)
                    MainComponent.Event.ProfileTabClick ->
                        navigation.bringToFront(MainComponent.Tab.Profile)
                    MainComponent.Event.NotesTabClick ->
                        navigation.bringToFront(MainComponent.Tab.Notes)
                }
            }
        )

    private fun createChild(
        tab: MainComponent.Tab,
        componentContext: AppComponentContext,
    ): ScreenChild =
        when (tab) {
            MainComponent.Tab.Home ->
                homeComponentFactory
                    .create(componentContext = componentContext)
                    .asChild(homeScreen::Content)

            MainComponent.Tab.Notes ->
                notesComponentFactory
                    .create(componentContext = componentContext)
                    .asChild(notesScreen::Content)

            MainComponent.Tab.Profile ->
                profileComponentFactory
                    .create(componentContext = componentContext, onLogout = onLogout)
                    .asChild(profileScreen::Content)
        }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : MainComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            onLogout: () -> Unit,
        ): DefaultMainComponent
    }
}
