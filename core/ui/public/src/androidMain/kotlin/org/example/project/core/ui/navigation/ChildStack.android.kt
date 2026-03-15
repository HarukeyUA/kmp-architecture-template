package org.example.project.core.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.PathEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.PredictiveBackParams
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimator
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.stackAnimator
import com.arkivanov.decompose.extensions.compose.stack.animation.isFront
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackAnimatable
import com.arkivanov.essenty.backhandler.BackEvent
import com.arkivanov.essenty.backhandler.BackHandler
import kotlin.math.abs
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val MoveFactor = 0.2f
private const val ScrimMaxAlpha = 0.1f
private const val DurationMs = 450

private val EmphasizedEasing =
    PathEasing(
        Path().apply {
            moveTo(0f, 0f)
            cubicTo(0.05f, 0f, 0.133333f, 0.06f, 0.166666f, 0.4f)
            cubicTo(0.208333f, 0.82f, 0.25f, 1f, 1f, 1f)
        }
    )

@OptIn(ExperimentalDecomposeApi::class)
actual fun <C : Any, T : Any> backAnimation(
    backHandler: BackHandler,
    onBackClick: () -> Unit,
): StackAnimation<C, T> =
    stackAnimation(
        animator = materialSharedAxisX(),
        predictiveBackParams = {
            PredictiveBackParams(
                backHandler = backHandler,
                onBack = onBackClick,
                animatable = ::materialSharedAxisXAnimatable,
            )
        },
    )

@OptIn(ExperimentalDecomposeApi::class)
private fun materialSharedAxisX(): StackAnimator =
    stackAnimator(animationSpec = tween(durationMillis = DurationMs, easing = EmphasizedEasing)) {
        factor,
        direction ->
        Modifier.graphicsLayer {
            translationX = size.width * if (direction.isFront) factor else factor * MoveFactor
            alpha = if (direction.isFront) 1f - abs(factor) else 1f
            compositingStrategy = CompositingStrategy.Offscreen
        }
    }

@OptIn(ExperimentalDecomposeApi::class)
private fun materialSharedAxisXAnimatable(initialBackEvent: BackEvent): PredictiveBackAnimatable =
    MaterialSharedAxisXAnimatable(initialBackEvent)

@OptIn(ExperimentalDecomposeApi::class)
private class MaterialSharedAxisXAnimatable(initialEvent: BackEvent) : PredictiveBackAnimatable {
    private val finishAnimatable = Animatable(0f)
    private val progressAnimatable = Animatable(initialEvent.progress)

    override val exitModifier: Modifier =
        Modifier.graphicsLayer {
            val progress = progressAnimatable.value
            val finishProgress = finishAnimatable.value

            translationX =
                lerp(
                    start = size.width * MoveFactor * progress,
                    stop = size.width,
                    fraction = finishProgress,
                )
            alpha = 1f - finishProgress
            compositingStrategy = CompositingStrategy.Offscreen
        }

    override val enterModifier: Modifier =
        Modifier.drawWithContent {
                drawContent()
                val scrimAlpha = (1f - finishAnimatable.value) * ScrimMaxAlpha
                drawRect(color = Color.Black.copy(alpha = scrimAlpha))
            }
            .graphicsLayer {
                val progress = progressAnimatable.value
                val finishProgress = finishAnimatable.value

                translationX =
                    lerp(
                        start = -size.width * MoveFactor * (1f - progress),
                        stop = 0f,
                        fraction = finishProgress,
                    )
                compositingStrategy = CompositingStrategy.Offscreen
            }

    override suspend fun animate(event: BackEvent) {
        progressAnimatable.snapTo(event.progress)
    }

    override suspend fun finish() {
        coroutineScope {
            val velocity = progressAnimatable.velocity
            val currentProgress = progressAnimatable.value
            launch {
                progressAnimatable.animateTo(
                    targetValue = currentProgress + (1f - currentProgress) * velocity,
                    initialVelocity = velocity,
                )
            }
            launch {
                finishAnimatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = DurationMs, easing = EmphasizedEasing),
                )
            }
        }
    }

    override suspend fun cancel() {
        progressAnimatable.animateTo(0f)
    }
}
