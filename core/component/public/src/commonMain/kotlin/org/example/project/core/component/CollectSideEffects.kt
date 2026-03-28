package org.example.project.core.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

@Composable
fun <SE : UiSideEffect> CollectSideEffects(
    sideEffects: SideEffects<SE>,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onSideEffect: suspend (SE) -> Unit,
) {
    val updatedOnSideEffect by rememberUpdatedState(onSideEffect)
    LaunchedEffect(sideEffects, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            sideEffects.flow.collect { updatedOnSideEffect(it) }
        }
    }
}
