package org.example.project.core.component.internal

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.savedstate.savedState
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import kotlin.test.Test

class StateKeeperSaveableStateRegistryTest {

    private companion object {
        const val REGISTRY_KEY = "test-registry"
    }

    private val platformSavesArbitraryObjects =
        PlatformSavedStateRegistryUtils.canBeSaved(object {})

    @Test
    fun `canBeSaved returns true for Int`() {
        val registry = createRegistry()
        assertThat(registry.canBeSaved(42)).isTrue()
    }

    @Test
    fun `canBeSaved returns true for String`() {
        val registry = createRegistry()
        assertThat(registry.canBeSaved("hello")).isTrue()
    }

    @Test
    fun `canBeSaved returns true for Boolean`() {
        val registry = createRegistry()
        assertThat(registry.canBeSaved(true)).isTrue()
    }

    @Test
    fun `canBeSaved returns true for Float`() {
        val registry = createRegistry()
        assertThat(registry.canBeSaved(1.5f)).isTrue()
    }

    @Test
    fun `canBeSaved returns true for Long`() {
        val registry = createRegistry()
        assertThat(registry.canBeSaved(100L)).isTrue()
    }

    @Test
    fun `canBeSaved returns true for Double`() {
        val registry = createRegistry()
        assertThat(registry.canBeSaved(2.71)).isTrue()
    }

    @Test
    fun `canBeSaved returns true for Char`() {
        val registry = createRegistry()
        assertThat(registry.canBeSaved('A')).isTrue()
    }

    @Test
    fun `canBeSaved returns true for Byte`() {
        val registry = createRegistry()
        assertThat(registry.canBeSaved(1.toByte())).isTrue()
    }

    @Test
    fun `canBeSaved returns true for Short`() {
        val registry = createRegistry()
        assertThat(registry.canBeSaved(1.toShort())).isTrue()
    }

    @Test
    fun `canBeSaved returns true for SnapshotMutableState with structural equality policy`() {
        val registry = createRegistry()
        val state = mutableStateOf(42, structuralEqualityPolicy())
        assertThat(registry.canBeSaved(state)).isTrue()
    }

    @Test
    fun `canBeSaved returns true for SnapshotMutableState with referential equality policy`() {
        val registry = createRegistry()
        val state = mutableStateOf(42, referentialEqualityPolicy())
        assertThat(registry.canBeSaved(state)).isTrue()
    }

    @Test
    fun `canBeSaved returns true for SnapshotMutableState with never equal policy`() {
        val registry = createRegistry()
        val state = mutableStateOf(42, neverEqualPolicy())
        assertThat(registry.canBeSaved(state)).isTrue()
    }

    @Test
    fun `canBeSaved returns true for SnapshotMutableState with null value`() {
        val registry = createRegistry()
        val state = mutableStateOf<Int?>(null)
        assertThat(registry.canBeSaved(state)).isTrue()
    }

    @Test
    fun `canBeSaved for SnapshotMutableState wrapping arbitrary type depends on platform`() {
        val registry = createRegistry()
        val arbitrary = object {}
        val state = mutableStateOf(arbitrary)
        assertThat(registry.canBeSaved(state)).isEqualTo(platformSavesArbitraryObjects)
    }

    @Test
    fun `canBeSaved returns true for SnapshotMutableState wrapping String`() {
        val registry = createRegistry()
        val state = mutableStateOf("hello")
        assertThat(registry.canBeSaved(state)).isTrue()
    }

    @Test
    fun `canBeSaved returns true for list of supported values`() {
        val registry = createRegistry()
        assertThat(registry.canBeSaved(listOf(1, "two", true))).isTrue()
    }

    @Test
    fun `canBeSaved returns true for empty list`() {
        val registry = createRegistry()
        assertThat(registry.canBeSaved(emptyList<Any>())).isTrue()
    }

    @Test
    fun `canBeSaved for list containing arbitrary type depends on platform`() {
        val registry = createRegistry()
        val arbitrary = object {}
        assertThat(registry.canBeSaved(listOf(1, arbitrary)))
            .isEqualTo(platformSavesArbitraryObjects)
    }

    @Test
    fun `canBeSaved returns true for list with null values`() {
        val registry = createRegistry()
        assertThat(registry.canBeSaved(listOf(1, null, "hello"))).isTrue()
    }

    @Test
    fun `canBeSaved returns true for map of supported values`() {
        val registry = createRegistry()
        assertThat(registry.canBeSaved(mapOf("a" to 1, "b" to "two"))).isTrue()
    }

    @Test
    fun `canBeSaved returns true for empty map`() {
        val registry = createRegistry()
        assertThat(registry.canBeSaved(emptyMap<String, Any>())).isTrue()
    }

    @Test
    fun `canBeSaved for map containing arbitrary type depends on platform`() {
        val registry = createRegistry()
        val arbitrary = object {}
        assertThat(registry.canBeSaved(mapOf("a" to arbitrary)))
            .isEqualTo(platformSavesArbitraryObjects)
    }

    @Test
    fun `canBeSaved returns true for map with null values`() {
        val registry = createRegistry()
        assertThat(registry.canBeSaved(mapOf("a" to null, "b" to 1))).isTrue()
    }

    @Test
    fun `canBeSaved returns true for nested list of supported values`() {
        val registry = createRegistry()
        assertThat(registry.canBeSaved(listOf(listOf(1, 2), listOf("a", "b")))).isTrue()
    }

    @Test
    fun `canBeSaved for nested list containing arbitrary type depends on platform`() {
        val registry = createRegistry()
        val arbitrary = object {}
        assertThat(registry.canBeSaved(listOf(listOf(arbitrary))))
            .isEqualTo(platformSavesArbitraryObjects)
    }

    @Test
    fun `canBeSaved returns true for SavedState`() {
        val registry = createRegistry()
        val state = savedState(mapOf("x" to 10))
        assertThat(registry.canBeSaved(state)).isTrue()
    }

    @Test
    fun `canBeSaved returns true for empty SavedState`() {
        val registry = createRegistry()
        assertThat(registry.canBeSaved(savedState())).isTrue()
    }

    @Test
    fun `canBeSaved for arbitrary type depends on platform`() {
        val registry = createRegistry()
        val arbitrary = object {}
        assertThat(registry.canBeSaved(arbitrary)).isEqualTo(platformSavesArbitraryObjects)
    }

    @Test
    fun `registerProvider and consumeRestored work for basic value`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = StateKeeperSaveableStateRegistry(stateKeeper, REGISTRY_KEY)

        registry.registerProvider("myKey") { 42 }

        val saved = registry.performSave()
        assertThat(saved["myKey"]).isNotNull().containsExactly(42)

        registry.unregister()
    }

    @Test
    fun `consumeRestored returns null for unknown key`() {
        val registry = createRegistry()
        assertThat(registry.consumeRestored("nonexistent")).isNull()
        registry.unregister()
    }

    @Test
    fun `consumeRestored returns null on second call for same key`() {
        val stateKeeper = StateKeeperDispatcher()
        val first = StateKeeperSaveableStateRegistry(stateKeeper, REGISTRY_KEY)
        first.registerProvider("myKey") { "hello" }
        val savedState = stateKeeper.save()
        first.unregister()

        val restored = StateKeeperDispatcher(savedState)
        val second = StateKeeperSaveableStateRegistry(restored, REGISTRY_KEY)

        assertThat(second.consumeRestored("myKey")).isNotNull()
        assertThat(second.consumeRestored("myKey")).isNull()

        second.unregister()
    }

    @Test
    fun `state survives save and restore cycle`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = StateKeeperSaveableStateRegistry(stateKeeper, REGISTRY_KEY)

        registry.registerProvider("counter") { 5 }
        registry.registerProvider("name") { "test" }

        val savedState = stateKeeper.save()
        registry.unregister()

        val restoredStateKeeper = StateKeeperDispatcher(savedState)
        val restoredRegistry = StateKeeperSaveableStateRegistry(restoredStateKeeper, REGISTRY_KEY)

        assertThat(restoredRegistry.consumeRestored("counter")).isEqualTo(5)
        assertThat(restoredRegistry.consumeRestored("name")).isEqualTo("test")

        restoredRegistry.unregister()
    }

    @Test
    fun `multiple providers for same key accumulate values`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = StateKeeperSaveableStateRegistry(stateKeeper, REGISTRY_KEY)

        registry.registerProvider("key") { 1 }
        registry.registerProvider("key") { 2 }

        val saved = registry.performSave()
        assertThat(saved["key"]).isNotNull().containsExactly(1, 2)

        registry.unregister()
    }

    @Test
    fun `state with null value survives save and restore cycle`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = StateKeeperSaveableStateRegistry(stateKeeper, REGISTRY_KEY)

        registry.registerProvider("nullable") { null }

        val savedState = stateKeeper.save()
        registry.unregister()

        val restoredStateKeeper = StateKeeperDispatcher(savedState)
        val restoredRegistry = StateKeeperSaveableStateRegistry(restoredStateKeeper, REGISTRY_KEY)

        assertThat(restoredRegistry.consumeRestored("nullable")).isNull()

        restoredRegistry.unregister()
    }

    @Test
    fun `complex state survives save and restore cycle`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = StateKeeperSaveableStateRegistry(stateKeeper, REGISTRY_KEY)

        registry.registerProvider("int") { 42 }
        registry.registerProvider("string") { "hello" }
        registry.registerProvider("bool") { true }
        registry.registerProvider("list") { listOf(1, 2, 3) }

        val savedState = stateKeeper.save()
        registry.unregister()

        val restoredStateKeeper = StateKeeperDispatcher(savedState)
        val restoredRegistry = StateKeeperSaveableStateRegistry(restoredStateKeeper, REGISTRY_KEY)

        assertThat(restoredRegistry.consumeRestored("int")).isEqualTo(42)
        assertThat(restoredRegistry.consumeRestored("string")).isEqualTo("hello")
        assertThat(restoredRegistry.consumeRestored("bool")).isEqualTo(true)
        assertThat(restoredRegistry.consumeRestored("list")).isEqualTo(listOf(1, 2, 3))

        restoredRegistry.unregister()
    }

    @Test
    fun `unregister removes registration from stateKeeper`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = StateKeeperSaveableStateRegistry(stateKeeper, REGISTRY_KEY)

        assertThat(stateKeeper.isRegistered(REGISTRY_KEY)).isTrue()

        registry.unregister()

        assertThat(stateKeeper.isRegistered(REGISTRY_KEY)).isFalse()
    }

    @Test
    fun `creating registry after unregister works`() {
        val stateKeeper = StateKeeperDispatcher()

        val first = StateKeeperSaveableStateRegistry(stateKeeper, REGISTRY_KEY)
        first.registerProvider("key") { "first" }
        first.unregister()

        val second = StateKeeperSaveableStateRegistry(stateKeeper, REGISTRY_KEY)
        second.registerProvider("key") { "second" }

        val saved = second.performSave()
        assertThat(saved["key"]).isNotNull().containsExactly("second")

        second.unregister()
    }

    @Test
    fun `entry unregister removes provider`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = StateKeeperSaveableStateRegistry(stateKeeper, REGISTRY_KEY)

        val entry = registry.registerProvider("key") { 42 }
        entry.unregister()

        val saved = registry.performSave()
        assertThat(saved["key"]).isNull()

        registry.unregister()
    }

    @Test
    fun `entry unregister removes only that provider`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = StateKeeperSaveableStateRegistry(stateKeeper, REGISTRY_KEY)

        val entry1 = registry.registerProvider("key") { 1 }
        registry.registerProvider("key") { 2 }
        entry1.unregister()

        val saved = registry.performSave()
        assertThat(saved["key"]).isNotNull().containsExactly(2)

        registry.unregister()
    }

    @Test
    fun `different registry keys are independent`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry1 = StateKeeperSaveableStateRegistry(stateKeeper, "key1")
        val registry2 = StateKeeperSaveableStateRegistry(stateKeeper, "key2")

        registry1.registerProvider("value") { "from-1" }
        registry2.registerProvider("value") { "from-2" }

        val savedState = stateKeeper.save()
        registry1.unregister()
        registry2.unregister()

        val restoredStateKeeper = StateKeeperDispatcher(savedState)
        val restored1 = StateKeeperSaveableStateRegistry(restoredStateKeeper, "key1")
        val restored2 = StateKeeperSaveableStateRegistry(restoredStateKeeper, "key2")

        assertThat(restored1.consumeRestored("value")).isEqualTo("from-1")
        assertThat(restored2.consumeRestored("value")).isEqualTo("from-2")

        restored1.unregister()
        restored2.unregister()
    }

    @Test
    fun `registry with no restored state returns null for consumeRestored`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = StateKeeperSaveableStateRegistry(stateKeeper, REGISTRY_KEY)

        assertThat(registry.consumeRestored("anyKey")).isNull()

        registry.unregister()
    }

    @Test
    fun `performSave with no registered providers returns empty map`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = StateKeeperSaveableStateRegistry(stateKeeper, REGISTRY_KEY)

        val saved = registry.performSave()
        assertThat(saved).isEqualTo(emptyMap())

        registry.unregister()
    }

    private fun createRegistry(
        stateKeeper: StateKeeperDispatcher = StateKeeperDispatcher()
    ): StateKeeperSaveableStateRegistry {
        return StateKeeperSaveableStateRegistry(stateKeeper, REGISTRY_KEY)
    }
}
