package org.example.project.core.component.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.runtime.structuralEqualityPolicy
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.statekeeper.StateKeeper

internal class StateKeeperSaveableStateRegistry(
    private val stateKeeper: StateKeeper,
    private val key: String
) : SaveableStateRegistry {

    private val restored: Map<String, List<Any?>>? =
        stateKeeper.consume(key, SavedStateSerializer)

    private val registry = SaveableStateRegistry(restored) { canBeSaved(it) }

    init {
        stateKeeper.register(key, SavedStateSerializer) {
            registry.performSave()
        }
    }

    override fun canBeSaved(value: Any): Boolean {
        if (value is SnapshotMutableState<*>) {
            if (
                value.policy === neverEqualPolicy<Any?>() ||
                value.policy === structuralEqualityPolicy<Any?>() ||
                value.policy === referentialEqualityPolicy<Any?>()
            ) {
                val stateValue = value.value
                return if (stateValue == null) true else canBeSaved(stateValue)
            } else {
                return false
            }
        }

        return isSupported(value)
    }

    private fun isSupported(value: Any): Boolean {
        return when (value) {
            is Int, is String, is Boolean, is Float, is Long, is Double, is Char, is Byte, is Short -> true
            is List<*> -> {
                value.none { item -> item != null && !canBeSaved(item) }
            }

            is Map<*, *> -> {
                value.none { (key, item) ->
                    key !is String && item != null && !canBeSaved(item)
                }
            }

            else -> PlatformSavedStateRegistryUtils.canBeSaved(value)
        }
    }

    override fun consumeRestored(key: String): Any? {
        return registry.consumeRestored(key)
    }

    override fun performSave(): Map<String, List<Any?>> {
        return registry.performSave()
    }

    override fun registerProvider(
        key: String,
        valueProvider: () -> Any?
    ): SaveableStateRegistry.Entry {
        return registry.registerProvider(key, valueProvider)
    }

    fun unregister() {
        stateKeeper.unregister(key)
    }
}

@Composable
internal fun <T> ComponentContext.ProvideStateKeeperSaveableStateRegistry(
    key: String = "state-keeper-state-registry",
    content: @Composable () -> T,
): T {
    val registry = remember(stateKeeper, key) {
        StateKeeperSaveableStateRegistry(stateKeeper, key = key)
    }

    DisposableEffect(registry) {
        onDispose {
            registry.unregister()
        }
    }

    return returningCompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
        content()
    }
}
