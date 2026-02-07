package org.example.project.feature.home

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ExperimentalDecomposeApi
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.example.project.core.ui.navigation.ChildStack

@ContributesBinding(AppScope::class)
@Inject
class DefaultHomeScreen(
    private val homeListScreen: HomeListScreen,
    private val homeDetailScreen: HomeDetailScreen,
) : HomeScreen {

    @OptIn(ExperimentalDecomposeApi::class)
    @Composable
    override fun Content(component: HomeComponent) {
        ChildStack(component) { child ->
            when (val instance = child.instance) {
                is HomeComponent.Child.List -> homeListScreen.Content(instance.component)
                is HomeComponent.Child.Detail -> homeDetailScreen.Content(instance.component)
            }
        }
    }
}
