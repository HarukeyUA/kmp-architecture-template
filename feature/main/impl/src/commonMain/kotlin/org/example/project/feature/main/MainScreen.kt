package org.example.project.feature.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
        val pagesState by component.pages.subscribeAsState()

        Scaffold(
            bottomBar = {
                NavigationBar {
                    TabItem.entries.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = pagesState.selectedIndex == index,
                            onClick = { component.selectPage(index) },
                            icon = { Text(tab.icon) },
                            label = { Text(tab.title) },
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                val selectedPage = pagesState.items.getOrNull(pagesState.selectedIndex)?.instance
                when (selectedPage) {
                    is MainComponent.Child.Home -> homeScreen.Content(selectedPage.component)
                    is MainComponent.Child.Search -> searchScreen.Content(selectedPage.component)
                    is MainComponent.Child.Profile -> profileScreen.Content(selectedPage.component)
                    null -> {}
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
