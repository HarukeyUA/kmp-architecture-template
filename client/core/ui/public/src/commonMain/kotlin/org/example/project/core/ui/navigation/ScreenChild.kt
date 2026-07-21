package org.example.project.core.ui.navigation

import androidx.compose.runtime.Composable

/**
 * A self-rendering child for use with
 * [StackComponent][org.example.project.core.navigation.StackComponent].
 *
 * Pairs a child component with its screen rendering so that host screens don't need to know about
 * individual child types. Create instances in the component's child factory function.
 */
fun interface ScreenChild {
    @Composable fun Content()
}

/** Pairs this component with its [screen] rendering to produce a [ScreenChild]. */
fun <T> T.asChild(screen: @Composable (T) -> Unit): ScreenChild = ScreenChild { screen(this) }
