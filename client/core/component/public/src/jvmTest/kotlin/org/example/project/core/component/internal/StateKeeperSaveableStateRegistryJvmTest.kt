package org.example.project.core.component.internal

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import java.io.Serializable
import kotlin.test.Test
import kotlinx.serialization.json.Json

class StateKeeperSaveableStateRegistryJvmTest {

    // A lazy layout keys each item's saveables by a type of its own, and Compose refuses to
    // compose the item if the registry above it will not take that key; a desktop has no Bundle
    // to refuse it for.
    @Test
    fun `a key of a type nothing writes is still accepted`() {
        val registry = StateKeeperSaveableStateRegistry("test-registry")
        registry.attach(StateKeeperDispatcher())
        assertThat(registry.canBeSaved(object {})).isTrue()
    }

    @Test
    fun `accepting a value at composition is not the same as writing it`() {
        assertThat(PlatformSavedStateRegistryUtils.isWritable(object {})).isFalse()
        assertThat(PlatformSavedStateRegistryUtils.isWritable(Thread.State.NEW)).isTrue()
    }

    @Test
    fun `Serializable on the outer class is not taken at its word`() {
        assertThat(PlatformSavedStateRegistryUtils.isWritable(HoldsAField(Any()))).isFalse()
        assertThat(PlatformSavedStateRegistryUtils.isWritable(HoldsAField("fine"))).isTrue()
    }

    @Test
    fun `a Serializable that fails deeper down costs the whole saveable, not a slot inside it`() {
        val input =
            mapOf(
                "top" to listOf(HoldsAField(Any())),
                "nested" to listOf(listOf(1, HoldsAField(Any()), 3)),
                "kept" to listOf(HoldsAField("fine")),
            )

        val encoded = Json.encodeToString(SavedStateSerializer, input)
        val result = Json.decodeFromString(SavedStateSerializer, encoded)

        assertThat(result["top"]).isEqualTo(listOf(null))
        assertThat(result["nested"]).isEqualTo(listOf(null))
        assertThat((result["kept"]!!.single() as HoldsAField).field).isEqualTo("fine")
    }

    private data class HoldsAField(val field: Any) : Serializable {
        private companion object {
            private const val serialVersionUID = 1L
        }
    }
}
