package org.example.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.stackAnimation
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.example.project.core.component.snackbar.rememberDispatchedSnackbarHostState
import org.example.project.core.ui.error.CompositeErrorRenderer
import org.example.project.core.ui.error.LocalErrorRenderer
import org.example.project.core.ui.navigation.ChildStack
import org.example.project.core.ui.navigation.defaultPredictiveBackParams
import org.example.project.core.ui.navigation.defaultStackAnimator
import org.example.project.core.ui.theme.AppTheme
import org.example.project.feature.auth.LoginScreen
import org.example.project.feature.main.presentation.MainScreen
import org.example.project.feature.splash.presentation.SplashScreen

@ContributesBinding(AppScope::class)
@Inject
class DefaultRootScreen(
    private val splashScreen: SplashScreen,
    private val loginScreen: LoginScreen,
    private val mainScreen: MainScreen,
    private val errorRenderer: CompositeErrorRenderer,
) : RootScreen {

    @OptIn(ExperimentalDecomposeApi::class)
    @Composable
    override fun Content(component: RootComponent) {
        val composeSnackbarHostState =
            rememberDispatchedSnackbarHostState(component.snackbarHostState)

        AppTheme {
            CompositionLocalProvider(LocalErrorRenderer provides errorRenderer) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ChildStack(
                            modifier = Modifier.fillMaxSize(),
                            component = component,
                            animation =
                                stackAnimation(
                                    predictiveBackParams = {
                                        defaultPredictiveBackParams(
                                            backHandler = component.backHandler,
                                            onBackClick = component::onBackClick,
                                        )
                                    }
                                ) { child, otherChild, _, _ ->
                                    val isSplash =
                                        child.instance is RootComponent.Child.Splash ||
                                            otherChild.instance is RootComponent.Child.Splash
                                    if (isSplash) fade() else defaultStackAnimator()
                                },
                        ) {
                            when (val child = it.instance) {
                                is RootComponent.Child.Splash -> splashScreen.Content()
                                is RootComponent.Child.Login -> loginScreen.Content(child.component)
                                is RootComponent.Child.Main -> mainScreen.Content(child.component)
                            }
                        }

                        SnackbarHost(
                            hostState = composeSnackbarHostState,
                            modifier =
                                Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
                        )
                    }
                }
            }
        }
    }
}
