package org.example.project.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimationScope
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.essenty.backhandler.BackHandler
import org.example.project.core.navigation.StackComponent

/**
 * Renders the active child from a [StackComponent] with optional animation.
 *
 * This is a convenience wrapper around Decompose's [Children] composable that simplifies the common
 * pattern of rendering a stack.
 *
 * Example usage:
 * ```
 * ChildStack(component) { child ->
 *     when (val instance = child.instance) {
 *         is HomeComponent.Child.List -> ListScreen(instance.component)
 *         is HomeComponent.Child.Detail -> DetailScreen(instance.component)
 *     }
 * }
 * ```
 *
 * @param component The [StackComponent] whose stack should be rendered
 * @param modifier Modifier to apply to the container
 * @param animation Stack animation to use for transitions, defaults to fade
 * @param content Composable to render for the active child
 */
@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun <C : Any, T : Any> ChildStack(
    component: StackComponent<C, T>,
    modifier: Modifier = Modifier,
    animation: StackAnimation<C, T>? =
        backAnimation(backHandler = component.backHandler, onBackClick = component::onBackClick),
    content: @Composable StackAnimationScope.(child: Child.Created<C, T>) -> Unit,
) {
    ChildStack(
        stack = component.stack,
        modifier = modifier,
        animation = animation,
        content = content,
    )
}

@OptIn(ExperimentalDecomposeApi::class)
expect fun <C : Any, T : Any> backAnimation(
    backHandler: BackHandler,
    onBackClick: () -> Unit,
): StackAnimation<C, T>
