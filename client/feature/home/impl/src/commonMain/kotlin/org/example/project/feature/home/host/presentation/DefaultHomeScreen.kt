package org.example.project.feature.home.host.presentation

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ExperimentalDecomposeApi
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.example.project.core.ui.navigation.ChildStack

@ContributesBinding(AppScope::class)
@Inject
class DefaultHomeScreen : HomeScreen {

    @OptIn(ExperimentalDecomposeApi::class)
    @Composable
    override fun Content(component: HomeComponent) {
        ChildStack(component)
    }
}
