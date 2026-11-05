package org.example.project.core.ui.topbar

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Wires [scrollBehavior] to page content like `.nestedScroll(...)`, and also re-expands a collapsed
 * bar from desktop mouse-wheel deltas: Compose Multiplatform stops consuming wheel events once
 * [scrollableState] is at the top, so the connection never sees them. Inert on touch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("CognitiveComplexMethod", "LoopWithTooManyJumpStatements")
fun Modifier.topAppBarNestedScroll(
    scrollBehavior: TopAppBarScrollBehavior,
    scrollableState: ScrollableState,
    wheelExpansionSensitivity: Dp = 5.dp,
): Modifier {
    val nestedScrollModifier = this.nestedScroll(scrollBehavior.nestedScrollConnection)

    if (scrollBehavior.isPinned) return nestedScrollModifier

    return nestedScrollModifier.pointerInput(
        scrollBehavior,
        scrollableState,
        wheelExpansionSensitivity,
    ) {
        val sensitivityPx = wheelExpansionSensitivity.toPx()
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                if (event.type != PointerEventType.Scroll) continue

                val state = scrollBehavior.state
                if (state.heightOffset >= 0f || scrollableState.canScrollBackward) continue

                val unconsumedDeltaY =
                    event.changes
                        .sumOf { change ->
                            if (change.isConsumed) 0.0 else change.scrollDelta.y.toDouble()
                        }
                        .toFloat()

                // Expanding only on negative (upward) delta avoids accidental expansion when
                // scrolling down on empty lists.
                if (unconsumedDeltaY < 0f) {
                    val expandPx = sensitivityPx * abs(unconsumedDeltaY)
                    state.heightOffset =
                        (state.heightOffset + expandPx).coerceIn(
                            minimumValue = state.heightOffsetLimit,
                            maximumValue = 0f,
                        )
                    event.changes.forEach { it.consume() }
                }
            }
        }
    }
}
