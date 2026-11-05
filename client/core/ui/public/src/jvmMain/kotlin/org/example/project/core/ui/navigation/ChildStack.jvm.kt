package org.example.project.core.ui.navigation

import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.PredictiveBackParams
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimator
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.stackAnimation
import com.arkivanov.essenty.backhandler.BackHandler

@OptIn(ExperimentalDecomposeApi::class)
actual fun <C : Any, T : Any> backAnimation(
    backHandler: BackHandler,
    onBackClick: () -> Unit,
): StackAnimation<C, T> =
    stackAnimation(
        animator = predictiveSlideStackAnimator(),
        predictiveBackParams = { defaultPredictiveBackParams(backHandler, onBackClick) },
    )

@OptIn(ExperimentalDecomposeApi::class)
actual fun defaultPredictiveBackParams(
    backHandler: BackHandler,
    onBackClick: () -> Unit,
): PredictiveBackParams? =
    PredictiveBackParams(
        backHandler = backHandler,
        onBack = onBackClick,
        animatable = ::predictiveSlideAnimatable,
    )

@OptIn(ExperimentalDecomposeApi::class)
actual fun defaultStackAnimator(): StackAnimator = predictiveSlideStackAnimator()
