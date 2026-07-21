package org.example.project.core.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue

class LoadableState<T>(private val blockState: State<suspend () -> Loadable<T>>) {
    var result by mutableStateOf<Loadable<T>>(Loadable.Loading)
        private set

    suspend fun refresh() {
        result = Loadable.Loading
        result = blockState.value()
    }
}

@Composable
fun <T> rememberLoadable(vararg keys: Any?, block: suspend () -> Loadable<T>): LoadableState<T> {
    val blockState = rememberUpdatedState(block)

    val state = remember(*keys) { LoadableState(blockState) }
    LaunchedEffect(state) { state.refresh() }
    return state
}
