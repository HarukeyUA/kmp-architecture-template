package org.example.project

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimation
import com.arkivanov.essenty.backhandler.BackHandler

@Composable
fun App(
    rootComponent: RootComponent,
    rootScreen: RootScreen,
) {
    rootScreen.Content(rootComponent)
}

@OptIn(ExperimentalDecomposeApi::class)
expect fun <C : Any, T : Any> backAnimation(
    backHandler: BackHandler,
    onBack: () -> Unit,
): StackAnimation<C, T>
