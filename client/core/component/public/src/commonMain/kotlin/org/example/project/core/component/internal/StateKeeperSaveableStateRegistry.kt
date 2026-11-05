package org.example.project.core.component.internal

import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.savedstate.SavedState
import arrow.core.Either
import co.touchlab.kermit.Logger
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.statekeeper.StateKeeper

private val log = Logger.withTag("SavedState")

/**
 * Main thread only, like the compositions it serves. Admission ([canBeSaved]) is deliberately wider
 * than fidelity: what a dump cannot carry is dropped at save time, see [SavedStateSerializer].
 */
internal class StateKeeperSaveableStateRegistry(private val key: String) :
    SaveableStateRegistry, InstanceKeeper.Instance {

    /** A value no live provider holds; [order] is its provider's registration rank here. */
    private class Stashed(val order: Int, val value: Any?)

    // The restored dump until a composition consumes it, and whatever each provider left behind on
    // its way out. Each key's list is ascending by order, restored values first.
    private val stashed: MutableMap<String, MutableList<Stashed>> = mutableMapOf()

    private val registry = SaveableStateRegistry(restoredValues = null) { canBeSaved(it) }

    // The keeper holding this registry's supplier: null before the first attach and after destroy.
    private var bound: StateKeeper? = null
    private var nextOrder = RESTORED + 1

    /**
     * Moves this registry onto [stateKeeper]: a rebuilt component arrives with a fresh StateKeeper
     * and the dump has to keep reaching the one that is saved. A key with anything stashed keeps
     * its values whole; the new keeper's dump only seeds keys nothing has stashed.
     */
    fun attach(stateKeeper: StateKeeper) {
        if (bound === stateKeeper) return
        for ((restoredKey, values) in consumeGuarded(stateKeeper)) {
            if (restoredKey !in stashed) {
                stashed[restoredKey] = values.mapTo(mutableListOf()) { Stashed(RESTORED, it) }
            }
        }
        stateKeeper.register(key, SavedStateSerializer) { performSave() }
        bound?.unregister(key)
        bound = stateKeeper
    }

    // Decoding is not resumable, so one saveable that does not decode costs every saveable beside
    // it under this key, but never the screen.
    private fun consumeGuarded(stateKeeper: StateKeeper): Map<String, List<Any?>> =
        Either.catch { stateKeeper.consume(key, SavedStateSerializer) }
            .onLeft { log.w(it) { "Dropped the dump under '$key'" } }
            .getOrNull()
            .orEmpty()

    override fun canBeSaved(value: Any): Boolean =
        when (value) {
            is SnapshotMutableState<*> -> value.hasSaveablePolicy() && canBeSavedOrNull(value.value)
            is Int,
            is String,
            is Boolean,
            is Float,
            is Long,
            is Double,
            is Char,
            is Byte,
            is Short,
            is SavedState -> true
            is List<*> -> value.all(::canBeSavedOrNull)
            is Map<*, *> -> value.values.all(::canBeSavedOrNull)
            else -> PlatformSavedStateRegistryUtils.canBeSaved(value)
        }

    private fun canBeSavedOrNull(value: Any?): Boolean = value == null || canBeSaved(value)

    private fun SnapshotMutableState<*>.hasSaveablePolicy(): Boolean =
        policy === neverEqualPolicy<Any?>() ||
            policy === structuralEqualityPolicy<Any?>() ||
            policy === referentialEqualityPolicy<Any?>()

    override fun consumeRestored(key: String): Any? {
        val values = stashed[key] ?: return null
        val first = values.removeAt(0)
        if (values.isEmpty()) stashed.remove(key)
        return first.value
    }

    /**
     * A key with a live provider drops that key's whole stash, as Compose's own registry drops
     * restored leftovers. A key whose stash is all null is left out: its slots restore as nothing.
     */
    override fun performSave(): Map<String, List<Any?>> {
        val departed =
            stashed
                .filterValues { values -> values.any { it.value != null } }
                .mapValues { (_, values) -> values.map { it.value } }
        return departed + registry.performSave()
    }

    override fun registerProvider(
        key: String,
        valueProvider: () -> Any?,
    ): SaveableStateRegistry.Entry {
        val entry = registry.registerProvider(key, valueProvider)
        val order = nextOrder++

        return object : SaveableStateRegistry.Entry {
            override fun unregister() {
                // A provider leaving usually means the composition is going, not the value: the
                // component can outlive it and be handed a new one, as every Android configuration
                // change does. Kept by rank, nulls too, so the n-th back gets the n-th.
                val values = stashed.getOrPut(key) { mutableListOf() }
                // A provider stood here, so the dump's leftovers under this key are superseded.
                values.removeAll { it.order == RESTORED }
                values.add(Stashed(order, valueProvider()))
                values.sortBy { it.order }
                entry.unregister()
            }
        }
    }

    override fun onDestroy() {
        bound?.unregister(key)
        bound = null
    }

    private companion object {
        /**
         * Below every provider's order: a restored value is the older one and goes back out first.
         */
        const val RESTORED = -1
    }
}
