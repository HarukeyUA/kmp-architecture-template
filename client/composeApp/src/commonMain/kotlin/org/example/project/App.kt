package org.example.project

import androidx.compose.runtime.Composable

@Composable
fun App(
    rootComponent: RootComponent,
    rootScreen: RootScreen,
    dynamicColorEnabled: Boolean = false,
) {
    rootScreen.Content(rootComponent, dynamicColorEnabled)
}
