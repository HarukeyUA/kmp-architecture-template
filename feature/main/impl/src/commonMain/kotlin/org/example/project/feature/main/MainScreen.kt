package org.example.project.feature.main

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.example.project.feature.home.HomeScreen
import org.example.project.feature.profile.ProfileScreen
import org.example.project.feature.search.SearchScreen

@ContributesBinding(AppScope::class)
@Inject
class DefaultMainScreen(
    private val homeScreen: HomeScreen,
    private val searchScreen: SearchScreen,
    private val profileScreen: ProfileScreen,
) : MainScreen {

    @Composable
    override fun Content(component: MainComponent) {
        val stack by component.stack.subscribeAsState()
        val activeChild = stack.active.instance

        Scaffold(
            bottomBar = {
                NavigationBar(modifier = Modifier.fillMaxWidth()) {
                    NavigationBarItem(
                        selected = activeChild is MainComponent.Child.Home,
                        onClick = { component.onEvent(MainComponent.Event.HomeTabClick) },
                        icon = { Text(TabItem.Home.icon) },
                        label = { Text(TabItem.Home.title) },
                    )
                    NavigationBarItem(
                        selected = activeChild is MainComponent.Child.Search,
                        onClick = { component.onEvent(MainComponent.Event.SearchTabClick) },
                        icon = { Text(TabItem.Search.icon) },
                        label = { Text(TabItem.Search.title) },
                    )
                    NavigationBarItem(
                        selected = activeChild is MainComponent.Child.Profile,
                        onClick = { component.onEvent(MainComponent.Event.ProfileTabClick) },
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
                when (val child = it.instance) {
                    is MainComponent.Child.Home -> homeScreen.Content(child.component)
                    is MainComponent.Child.Search -> searchScreen.Content(child.component)
                    is MainComponent.Child.Profile -> profileScreen.Content(child.component)
                }
            }
        }
    }
}

private enum class TabItem(val title: String, val icon: String) {
    Home("Home", "\uD83C\uDFE0"),
    Search("Search", "\uD83D\uDD0D"),
    Profile("Profile", "\uD83D\uDC64"),
}
