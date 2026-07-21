package org.example.project.feature.main.presentation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.example.project.core.component.snackbar.rememberDispatchedSnackbarHostState

@ContributesBinding(AppScope::class)
@Inject
class DefaultMainScreen : MainScreen {

    @Composable
    override fun Content(component: MainComponent) {
        val state by component.state.collectAsStateWithLifecycle()
        val stack by component.stack.subscribeAsState()
        val activeTab = stack.active.configuration
        val composeSnackbarHostState =
            rememberDispatchedSnackbarHostState(component.snackbarHostState)

        Scaffold(
            snackbarHost = { SnackbarHost(composeSnackbarHostState) },
            bottomBar = {
                NavigationBar(modifier = Modifier.fillMaxWidth()) {
                    NavigationBarItem(
                        selected = activeTab is MainComponent.Tab.Home,
                        onClick = { state.eventSink(MainComponent.Event.HomeTabClick) },
                        icon = { Text(TabItem.Home.icon) },
                        label = { Text(TabItem.Home.title) },
                    )
                    NavigationBarItem(
                        selected = activeTab is MainComponent.Tab.Profile,
                        onClick = { state.eventSink(MainComponent.Event.ProfileTabClick) },
                        icon = { Text(TabItem.Profile.icon) },
                        label = { Text(TabItem.Profile.title) },
                    )
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { paddingValue ->
            Children(
                modifier = Modifier.padding(paddingValue),
                stack = component.stack,
                animation = stackAnimation(fade()),
            ) {
                it.instance.Content()
            }
        }
    }
}

private enum class TabItem(val title: String, val icon: String) {
    Home("Home", "\uD83C\uDFE0"),
    Profile("Profile", "\uD83D\uDC64"),
}
