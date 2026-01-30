package org.example.project.core.component.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.currentComposer

/**
 * Similar to `CompositionLocalProvider` offered by the Compose runtime itself, but allows us to
 * return a result rather than returning `Unit`.
 */
@Composable
@OptIn(InternalComposeApi::class)
@Suppress("FunctionNaming")
internal fun <ModelT> returningCompositionLocalProvider(
    vararg values: ProvidedValue<*>,
    content: @Composable () -> ModelT,
): ModelT {
    currentComposer.startProviders(values)
    val model = content()
    currentComposer.endProviders()
    return model
}
