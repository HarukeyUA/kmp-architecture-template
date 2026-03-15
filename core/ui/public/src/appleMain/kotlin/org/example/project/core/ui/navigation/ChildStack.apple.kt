package org.example.project.core.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
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

private const val BackFactor = 1f / 3f
private const val ScrimMaxAlpha = 0.1f
private const val DurationMs = 350
private val IOSEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

@OptIn(ExperimentalDecomposeApi::class)
actual fun <C : Any, T : Any> backAnimation(
    backHandler: BackHandler,
    onBackClick: () -> Unit,
): StackAnimation<C, T> =
    stackAnimation(
        animator = iosSlide(),
        predictiveBackParams = {
            PredictiveBackParams(
                backHandler = backHandler,
                onBack = onBackClick,
                animatable = ::iosSlideAnimatable,
            )
        },
    )

@OptIn(ExperimentalDecomposeApi::class)
private fun iosSlide(): StackAnimator =
    stackAnimator(animationSpec = tween(durationMillis = DurationMs, easing = IOSEasing)) {
        factor,
        direction ->
        val scrimModifier =
            if (!direction.isFront) {
                Modifier.drawWithContent {
                    drawContent()
                    drawRect(color = Color.Black.copy(alpha = abs(factor) * ScrimMaxAlpha))
                }
            } else {
                Modifier
            }
        scrimModifier.graphicsLayer {
            translationX = size.width * if (direction.isFront) factor else factor * BackFactor
        }
    }

@OptIn(ExperimentalDecomposeApi::class)
private fun iosSlideAnimatable(initialBackEvent: BackEvent): PredictiveBackAnimatable =
    IOSSlideAnimatable(initialBackEvent)

@OptIn(ExperimentalDecomposeApi::class)
private class IOSSlideAnimatable(initialEvent: BackEvent) : PredictiveBackAnimatable {
    private val finishAnimatable = Animatable(0f)
    private val progressAnimatable = Animatable(initialEvent.progress)

    override val exitModifier: Modifier =
        Modifier.graphicsLayer {
            val progress = progressAnimatable.value
            val finishProgress = finishAnimatable.value

            translationX =
                lerp(start = size.width * progress, stop = size.width, fraction = finishProgress)
        }

    override val enterModifier: Modifier =
        Modifier.drawWithContent {
                drawContent()
                val progress = progressAnimatable.value
                val finishProgress = finishAnimatable.value
                val scrimAlpha = (1f - lerp(progress, 1f, finishProgress)) * ScrimMaxAlpha
                drawRect(color = Color.Black.copy(alpha = scrimAlpha))
            }
            .graphicsLayer {
                val progress = progressAnimatable.value
                val finishProgress = finishAnimatable.value

                translationX =
                    lerp(
                        start = -size.width * BackFactor * (1f - progress),
                        stop = 0f,
                        fraction = finishProgress,
                    )
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
                    targetValue =
                        currentProgress + (1f - currentProgress) * velocity.coerceAtMost(1f),
                    initialVelocity = velocity,
                )
            }
            launch {
                finishAnimatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = DurationMs, easing = IOSEasing),
                )
            }
        }
    }

    override suspend fun cancel() {
        progressAnimatable.animateTo(0f)
    }
}
