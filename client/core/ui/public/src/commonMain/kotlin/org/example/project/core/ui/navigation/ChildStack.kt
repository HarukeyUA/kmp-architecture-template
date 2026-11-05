package org.example.project.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.PredictiveBackParams
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimationScope
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimator
import com.arkivanov.essenty.backhandler.BackHandler
import org.example.project.core.navigation.StackComponent

/**
 * Renders the active child from a [StackComponent]. [animation] defaults to the platform back
 * animation.
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

/**
 * [ChildStack] for components whose children are [ScreenChild]s: each child renders itself, so no
 * `content` lambda is needed.
 */
@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun <C : Any> ChildStack(
    component: StackComponent<C, ScreenChild>,
    modifier: Modifier = Modifier,
    animation: StackAnimation<C, ScreenChild>? =
        backAnimation(backHandler = component.backHandler, onBackClick = component::onBackClick),
) {
    ChildStack(stack = component.stack, modifier = modifier, animation = animation) { child ->
        child.instance.Content()
    }
}

@OptIn(ExperimentalDecomposeApi::class)
expect fun <C : Any, T : Any> backAnimation(
    backHandler: BackHandler,
    onBackClick: () -> Unit,
): StackAnimation<C, T>

@ExperimentalDecomposeApi
expect fun defaultPredictiveBackParams(
    backHandler: BackHandler,
    onBackClick: () -> Unit,
): PredictiveBackParams?

@ExperimentalDecomposeApi expect fun defaultStackAnimator(): StackAnimator

/**
 * Where a committed back gesture lands, in the same 0f..1f progress the gesture reports. The
 * gesture is already committed by the time this runs, so the result never falls short of
 * [currentProgress] however hard the finger flicked backwards, and never overshoots 1f.
 */
internal fun settleTarget(currentProgress: Float, velocity: Float): Float =
    currentProgress + (1f - currentProgress) * velocity.coerceIn(0f, 1f)
