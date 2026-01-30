package org.example.project

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.example.project.feature.auth.LoginScreen
import org.example.project.feature.main.MainScreen

@ContributesBinding(AppScope::class)
@Inject
class DefaultRootScreen(
    private val splashScreen: SplashScreen,
    private val loginScreen: LoginScreen,
    private val mainScreen: MainScreen,
) : RootScreen {

    @OptIn(ExperimentalDecomposeApi::class)
    @Composable
    override fun Content(component: RootComponent) {
        MaterialTheme {
            ChildStack(
                stack = component.stack,
                animation =
                    backAnimation(
                        backHandler = component.backHandler,
                        onBack = component::onBackClick,
                    ),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    when (val child = it.instance) {
                        is RootComponent.Child.Splash -> splashScreen.Content()
                        is RootComponent.Child.Login -> loginScreen.Content(child.component)
                        is RootComponent.Child.Main -> mainScreen.Content(child.component)
                    }
                }
            }
        }
    }
}
