package org.example.project

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackGestureOverlay
import com.arkivanov.essenty.backhandler.BackDispatcher
import org.example.project.core.component.ProvideStateKeeperSaveableStateRegistry

@OptIn(ExperimentalDecomposeApi::class)
fun MainViewController(
    root: RootComponent,
    rootScreen: RootScreen,
    backDispatcher: BackDispatcher,
) = ComposeUIViewController {
    root.ProvideStateKeeperSaveableStateRegistry(key = "state-keeper-ui-registry") {
        PredictiveBackGestureOverlay(
            backDispatcher = backDispatcher,
            backIcon = null,
            modifier = Modifier.fillMaxSize(),
        ) {
            App(root, rootScreen)
        }
    }
}
