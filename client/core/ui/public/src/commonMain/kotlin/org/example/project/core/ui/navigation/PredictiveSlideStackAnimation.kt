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
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimator
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.stackAnimator
import com.arkivanov.decompose.extensions.compose.stack.animation.isFront
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackAnimatable
import com.arkivanov.essenty.backhandler.BackEvent
import kotlin.math.abs
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val BackFactor = 1f / 3f
private const val ScrimMaxAlpha = 0.1f
private const val DurationMs = 350
private val SlideEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

/**
 * iOS-style horizontal slide used by the Apple and JVM desktop targets: the entering screen slides
 * in from the trailing edge while the exiting one shifts by [BackFactor] under a fading scrim.
 */
@OptIn(ExperimentalDecomposeApi::class)
internal fun predictiveSlideStackAnimator(): StackAnimator =
    stackAnimator(animationSpec = tween(durationMillis = DurationMs, easing = SlideEasing)) {
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

/** Gesture-driven counterpart of [predictiveSlideStackAnimator] for predictive back. */
@OptIn(ExperimentalDecomposeApi::class)
internal fun predictiveSlideAnimatable(initialBackEvent: BackEvent): PredictiveBackAnimatable =
    PredictiveSlideAnimatable(initialBackEvent)

@OptIn(ExperimentalDecomposeApi::class)
private class PredictiveSlideAnimatable(initialEvent: BackEvent) : PredictiveBackAnimatable {
    private val finishAnimatable = Animatable(0f)
    private val progressAnimatable = Animatable(initialEvent.progress)

    override val exitModifier: Modifier = Modifier.graphicsLayer {
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
                    targetValue = settleTarget(currentProgress, velocity),
                    initialVelocity = velocity,
                )
            }
            launch {
                finishAnimatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = DurationMs, easing = SlideEasing),
                )
            }
        }
    }

    override suspend fun cancel() {
        progressAnimatable.animateTo(0f)
    }
}
