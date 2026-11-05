package org.example.project.core.component.internal

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.savedstate.savedState
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.arkivanov.essenty.statekeeper.SerializableContainer
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import kotlin.test.Test
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

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
        val registry = registryOn(stateKeeper, REGISTRY_KEY)

        registry.registerProvider("myKey") { 42 }

        val saved = registry.performSave()
        assertThat(saved["myKey"]).isNotNull().containsExactly(42)

        registry.onDestroy()
    }

    @Test
    fun `consumeRestored returns null for unknown key`() {
        val registry = createRegistry()
        assertThat(registry.consumeRestored("nonexistent")).isNull()
        registry.onDestroy()
    }

    @Test
    fun `consumeRestored returns null on second call for same key`() {
        val stateKeeper = StateKeeperDispatcher()
        val first = registryOn(stateKeeper, REGISTRY_KEY)
        first.registerProvider("myKey") { "hello" }
        val savedState = stateKeeper.save()
        first.onDestroy()

        val restored = StateKeeperDispatcher(savedState)
        val second = registryOn(restored, REGISTRY_KEY)

        assertThat(second.consumeRestored("myKey")).isNotNull()
        assertThat(second.consumeRestored("myKey")).isNull()

        second.onDestroy()
    }

    @Test
    fun `state survives save and restore cycle`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = registryOn(stateKeeper, REGISTRY_KEY)

        registry.registerProvider("counter") { 5 }
        registry.registerProvider("name") { "test" }

        val savedState = stateKeeper.save()
        registry.onDestroy()

        val restoredStateKeeper = StateKeeperDispatcher(savedState)
        val restoredRegistry = registryOn(restoredStateKeeper, REGISTRY_KEY)

        assertThat(restoredRegistry.consumeRestored("counter")).isEqualTo(5)
        assertThat(restoredRegistry.consumeRestored("name")).isEqualTo("test")

        restoredRegistry.onDestroy()
    }

    @Test
    fun `multiple providers for same key accumulate values`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = registryOn(stateKeeper, REGISTRY_KEY)

        registry.registerProvider("key") { 1 }
        registry.registerProvider("key") { 2 }

        val saved = registry.performSave()
        assertThat(saved["key"]).isNotNull().containsExactly(1, 2)

        registry.onDestroy()
    }

    @Test
    fun `state with null value survives save and restore cycle`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = registryOn(stateKeeper, REGISTRY_KEY)

        registry.registerProvider("nullable") { null }

        val savedState = stateKeeper.save()
        registry.onDestroy()

        val restoredStateKeeper = StateKeeperDispatcher(savedState)
        val restoredRegistry = registryOn(restoredStateKeeper, REGISTRY_KEY)

        assertThat(restoredRegistry.consumeRestored("nullable")).isNull()

        restoredRegistry.onDestroy()
    }

    @Test
    fun `complex state survives save and restore cycle`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = registryOn(stateKeeper, REGISTRY_KEY)

        registry.registerProvider("int") { 42 }
        registry.registerProvider("string") { "hello" }
        registry.registerProvider("bool") { true }
        registry.registerProvider("list") { listOf(1, 2, 3) }

        val savedState = stateKeeper.save()
        registry.onDestroy()

        val restoredStateKeeper = StateKeeperDispatcher(savedState)
        val restoredRegistry = registryOn(restoredStateKeeper, REGISTRY_KEY)

        assertThat(restoredRegistry.consumeRestored("int")).isEqualTo(42)
        assertThat(restoredRegistry.consumeRestored("string")).isEqualTo("hello")
        assertThat(restoredRegistry.consumeRestored("bool")).isEqualTo(true)
        assertThat(restoredRegistry.consumeRestored("list")).isEqualTo(listOf(1, 2, 3))

        restoredRegistry.onDestroy()
    }

    @Test
    fun `destroying the registry removes its registration from the stateKeeper`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = registryOn(stateKeeper, REGISTRY_KEY)

        assertThat(stateKeeper.isRegistered(REGISTRY_KEY)).isTrue()

        registry.onDestroy()

        assertThat(stateKeeper.isRegistered(REGISTRY_KEY)).isFalse()
    }

    @Test
    fun `creating a registry after the previous one was destroyed works`() {
        val stateKeeper = StateKeeperDispatcher()

        val first = registryOn(stateKeeper, REGISTRY_KEY)
        first.registerProvider("key") { "first" }
        first.onDestroy()

        val second = registryOn(stateKeeper, REGISTRY_KEY)
        second.registerProvider("key") { "second" }

        val saved = second.performSave()
        assertThat(saved["key"]).isNotNull().containsExactly("second")

        second.onDestroy()
    }

    @Test
    fun `entry unregister leaves the provider's value behind`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = registryOn(stateKeeper, REGISTRY_KEY)

        val entry = registry.registerProvider("key") { 42 }
        entry.unregister()

        val saved = registry.performSave()
        assertThat(saved["key"]).isNotNull().containsExactly(42)

        registry.onDestroy()
    }

    @Test
    fun `a live provider wins over the value a departed one left under the same key`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = registryOn(stateKeeper, REGISTRY_KEY)

        val entry1 = registry.registerProvider("key") { 1 }
        registry.registerProvider("key") { 2 }
        entry1.unregister()

        val saved = registry.performSave()
        assertThat(saved["key"]).isNotNull().containsExactly(2)

        registry.onDestroy()
    }

    @Test
    fun `different registry keys are independent`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry1 = registryOn(stateKeeper, "key1")
        val registry2 = registryOn(stateKeeper, "key2")

        registry1.registerProvider("value") { "from-1" }
        registry2.registerProvider("value") { "from-2" }

        val savedState = stateKeeper.save()
        registry1.onDestroy()
        registry2.onDestroy()

        val restoredStateKeeper = StateKeeperDispatcher(savedState)
        val restored1 = registryOn(restoredStateKeeper, "key1")
        val restored2 = registryOn(restoredStateKeeper, "key2")

        assertThat(restored1.consumeRestored("value")).isEqualTo("from-1")
        assertThat(restored2.consumeRestored("value")).isEqualTo("from-2")

        restored1.onDestroy()
        restored2.onDestroy()
    }

    @Test
    fun `registry with no restored state returns null for consumeRestored`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = registryOn(stateKeeper, REGISTRY_KEY)

        assertThat(registry.consumeRestored("anyKey")).isNull()

        registry.onDestroy()
    }

    @Test
    fun `a provider that leaves keeps its value for the next composition`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = registryOn(stateKeeper, REGISTRY_KEY)

        // The composition goes; the registry stays, the way it does across an Android
        // configuration change.
        registry.registerProvider("counter") { 5 }.unregister()

        assertThat(registry.consumeRestored("counter")).isEqualTo(5)
        registry.onDestroy()
    }

    @Test
    fun `what a provider left behind is still what the registry saves`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = registryOn(stateKeeper, REGISTRY_KEY)
        registry.registerProvider("counter") { 5 }.unregister()

        val savedState = stateKeeper.save()
        registry.onDestroy()

        val restored = registryOn(StateKeeperDispatcher(savedState), REGISTRY_KEY)
        assertThat(restored.consumeRestored("counter")).isEqualTo(5)
        restored.onDestroy()
    }

    @Test
    fun `performSave with no registered providers returns empty map`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = registryOn(stateKeeper, REGISTRY_KEY)

        val saved = registry.performSave()
        assertThat(saved).isEqualTo(emptyMap())

        registry.onDestroy()
    }

    @Test
    fun `providers sharing a key that leave in reverse are handed back in registration order`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = registryOn(stateKeeper, REGISTRY_KEY)

        val first = registry.registerProvider("key") { "first" }
        val second = registry.registerProvider("key") { "second" }
        val third = registry.registerProvider("key") { "third" }
        // Compose forgets what it remembered last first.
        third.unregister()
        second.unregister()
        first.unregister()

        assertThat(registry.performSave()["key"])
            .isNotNull()
            .containsExactly("first", "second", "third")
        assertThat(registry.consumeRestored("key")).isEqualTo("first")
        assertThat(registry.consumeRestored("key")).isEqualTo("second")
        assertThat(registry.consumeRestored("key")).isEqualTo("third")
        assertThat(registry.consumeRestored("key")).isNull()
        registry.onDestroy()
    }

    @Test
    fun `a provider that leaves later than a sibling with its key is handed back later`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = registryOn(stateKeeper, REGISTRY_KEY)

        val first = registry.registerProvider("key") { "first" }
        val second = registry.registerProvider("key") { "second" }
        first.unregister()
        second.unregister()

        assertThat(registry.performSave()["key"]).isNotNull().containsExactly("first", "second")
        registry.onDestroy()
    }

    @Test
    fun `attaching a new stateKeeper moves the registration and carries the dump along`() {
        val old = StateKeeperDispatcher()
        val registry = registryOn(old, REGISTRY_KEY)
        registry.registerProvider("counter") { 5 }.unregister()

        val fresh = StateKeeperDispatcher()
        registry.attach(fresh)

        assertThat(old.isRegistered(REGISTRY_KEY)).isFalse()
        assertThat(fresh.isRegistered(REGISTRY_KEY)).isTrue()
        val restored = registryOn(StateKeeperDispatcher(fresh.save()), REGISTRY_KEY)
        assertThat(restored.consumeRestored("counter")).isEqualTo(5)
        registry.onDestroy()
        restored.onDestroy()
        assertThat(fresh.isRegistered(REGISTRY_KEY)).isFalse()
    }

    @Test
    fun `attaching a stateKeeper with a dump keeps what is held and fills the gaps from the dump`() {
        val previous = StateKeeperDispatcher()
        val earlier = registryOn(previous, REGISTRY_KEY)
        earlier.registerProvider("counter") { 1 }
        earlier.registerProvider("only-in-dump") { "dump" }
        val saved = previous.save()
        earlier.onDestroy()

        val registry = registryOn(StateKeeperDispatcher(), REGISTRY_KEY)
        registry.registerProvider("counter") { 2 }.unregister()
        registry.attach(StateKeeperDispatcher(saved))

        assertThat(registry.consumeRestored("counter")).isEqualTo(2)
        assertThat(registry.consumeRestored("only-in-dump")).isEqualTo("dump")
        registry.onDestroy()
    }

    @Test
    fun `attaching the stateKeeper it is already on changes nothing`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = registryOn(stateKeeper, REGISTRY_KEY)
        registry.registerProvider("counter") { 5 }.unregister()

        registry.attach(stateKeeper)

        assertThat(stateKeeper.isRegistered(REGISTRY_KEY)).isTrue()
        assertThat(registry.consumeRestored("counter")).isEqualTo(5)
        registry.onDestroy()
    }

    @Test
    fun `a dump that does not decode is dropped instead of failing the registry`() {
        val previous = StateKeeperDispatcher()
        previous.register(REGISTRY_KEY, String.serializer()) { "not a dump" }
        val stateKeeper = StateKeeperDispatcher(previous.save().throughBytes())

        val registry = registryOn(stateKeeper, REGISTRY_KEY)

        assertThat(registry.consumeRestored("anything")).isNull()
        assertThat(stateKeeper.isRegistered(REGISTRY_KEY)).isTrue()
        registry.registerProvider("counter") { 5 }
        assertThat(registry.performSave()["counter"]).isNotNull().containsExactly(5)
        registry.onDestroy()
    }

    @Test
    fun `a dump survives the platform's bytes`() {
        val stateKeeper = StateKeeperDispatcher()
        val registry = registryOn(stateKeeper, REGISTRY_KEY)
        registry.registerProvider("state") { mutableStateOf(7) }
        registry.registerProvider("keyed") { mapOf(3 to "three") }
        val saved = stateKeeper.save().throughBytes()
        registry.onDestroy()

        val restored = registryOn(StateKeeperDispatcher(saved), REGISTRY_KEY)

        assertThat((restored.consumeRestored("state") as SnapshotMutableState<*>).value)
            .isEqualTo(7)
        assertThat(restored.consumeRestored("keyed")).isEqualTo(mapOf(3 to "three"))
        restored.onDestroy()
    }

    @Test
    fun `a provider that leaves with nothing still holds its slot among those sharing its key`() {
        val registry = createRegistry()

        val first = registry.registerProvider("key") { null }
        val second = registry.registerProvider("key") { "second" }
        second.unregister()
        first.unregister()

        assertThat(registry.performSave()["key"]).isNotNull().containsExactly(null, "second")
        assertThat(registry.consumeRestored("key")).isNull()
        assertThat(registry.consumeRestored("key")).isEqualTo("second")
        registry.onDestroy()
    }

    @Test
    fun `a key whose departed providers all held nothing is left out of the dump`() {
        val registry = createRegistry()
        registry.registerProvider("key") { null }.unregister()

        assertThat(registry.performSave()).isEqualTo(emptyMap())
        registry.onDestroy()
    }

    @Test
    fun `a provider leaving supersedes what the dump still held under its key`() {
        val previous = StateKeeperDispatcher()
        val earlier = registryOn(previous, REGISTRY_KEY)
        earlier.registerProvider("key") { "one" }
        earlier.registerProvider("key") { "two" }
        val saved = previous.save()
        earlier.onDestroy()

        // Only one of the two siblings composes this time; it leaves holding something newer.
        val registry = registryOn(StateKeeperDispatcher(saved), REGISTRY_KEY)
        assertThat(registry.consumeRestored("key")).isEqualTo("one")
        registry.registerProvider("key") { "newer" }.unregister()

        assertThat(registry.performSave()["key"]).isNotNull().containsExactly("newer")
        assertThat(registry.consumeRestored("key")).isEqualTo("newer")
        assertThat(registry.consumeRestored("key")).isNull()
        registry.onDestroy()
    }

    @Test
    fun `a registry never attached saves nothing and destroys quietly`() {
        val registry = StateKeeperSaveableStateRegistry(REGISTRY_KEY)
        registry.registerProvider("key") { 1 }
        assertThat(registry.performSave()["key"]).isNotNull().containsExactly(1)
        registry.onDestroy()
    }

    private fun registryOn(
        stateKeeper: StateKeeperDispatcher,
        key: String,
    ): StateKeeperSaveableStateRegistry =
        StateKeeperSaveableStateRegistry(key).apply { attach(stateKeeper) }

    private fun createRegistry(): StateKeeperSaveableStateRegistry =
        registryOn(StateKeeperDispatcher(), REGISTRY_KEY)

    private fun SerializableContainer.throughBytes(): SerializableContainer =
        Json.decodeFromString(
            SerializableContainer.serializer(),
            Json.encodeToString(SerializableContainer.serializer(), this),
        )
}
