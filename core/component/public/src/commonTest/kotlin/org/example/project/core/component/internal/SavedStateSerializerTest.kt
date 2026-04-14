package org.example.project.core.component.internal

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.savedstate.read
import androidx.savedstate.savedState
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.key
import kotlin.test.Test
import kotlinx.serialization.json.Json

class SavedStateSerializerTest {

    private val json = Json

    private fun roundtrip(value: Map<String, List<Any?>>): Map<String, List<Any?>> {
        val encoded = json.encodeToString(SavedStateSerializer, value)
        return json.decodeFromString(SavedStateSerializer, encoded)
    }

    @Test
    fun `empty map roundtrips`() {
        val result = roundtrip(emptyMap())
        assertThat(result).isEqualTo(emptyMap())
    }

    @Test
    fun `map with empty list roundtrips`() {
        val input = mapOf("key" to emptyList<Any?>())
        val result = roundtrip(input)
        assertThat(result).isEqualTo(input)
    }

    @Test
    fun `multiple keys roundtrip`() {
        val input =
            mapOf("first" to listOf(1), "second" to listOf("hello"), "third" to listOf(true))
        val result = roundtrip(input)
        assertThat(result).isEqualTo(input)
    }

    @Test
    fun `null value roundtrips`() {
        val input = mapOf("key" to listOf(null))
        val result = roundtrip(input)
        assertThat(result["key"]).isEqualTo(listOf(null))
    }

    @Test
    fun `list with multiple nulls roundtrips`() {
        val input = mapOf("key" to listOf(null, null, null))
        val result = roundtrip(input)
        assertThat(result["key"]).isEqualTo(listOf(null, null, null))
    }

    @Test
    fun `Int roundtrips`() {
        val input = mapOf("key" to listOf(42))
        assertThat(roundtrip(input)).isEqualTo(input)
    }

    @Test
    fun `Int edge values roundtrip`() {
        val input = mapOf("key" to listOf(Int.MIN_VALUE, 0, Int.MAX_VALUE))
        assertThat(roundtrip(input)).isEqualTo(input)
    }

    @Test
    fun `String roundtrips`() {
        val input = mapOf("key" to listOf("hello world"))
        assertThat(roundtrip(input)).isEqualTo(input)
    }

    @Test
    fun `empty String roundtrips`() {
        val input = mapOf("key" to listOf(""))
        assertThat(roundtrip(input)).isEqualTo(input)
    }

    @Test
    fun `Boolean roundtrips`() {
        val input = mapOf("key" to listOf(true, false))
        assertThat(roundtrip(input)).isEqualTo(input)
    }

    @Test
    fun `Float roundtrips`() {
        val input = mapOf("key" to listOf(3.14f))
        assertThat(roundtrip(input)).isEqualTo(input)
    }

    @Test
    fun `Float edge values roundtrip`() {
        val input = mapOf("key" to listOf(Float.MIN_VALUE, 0.0f, Float.MAX_VALUE))
        assertThat(roundtrip(input)).isEqualTo(input)
    }

    @Test
    fun `Long roundtrips`() {
        val input = mapOf("key" to listOf(123456789L))
        assertThat(roundtrip(input)).isEqualTo(input)
    }

    @Test
    fun `Long edge values roundtrip`() {
        val input = mapOf("key" to listOf(Long.MIN_VALUE, 0L, Long.MAX_VALUE))
        assertThat(roundtrip(input)).isEqualTo(input)
    }

    @Test
    fun `Double roundtrips`() {
        val input = mapOf("key" to listOf(2.718281828))
        assertThat(roundtrip(input)).isEqualTo(input)
    }

    @Test
    fun `Double edge values roundtrip`() {
        val input = mapOf("key" to listOf(Double.MIN_VALUE, 0.0, Double.MAX_VALUE))
        assertThat(roundtrip(input)).isEqualTo(input)
    }

    @Test
    fun `Char roundtrips`() {
        val input = mapOf("key" to listOf('A'))
        assertThat(roundtrip(input)).isEqualTo(input)
    }

    @Test
    fun `Byte roundtrips`() {
        val input = mapOf("key" to listOf(127.toByte()))
        assertThat(roundtrip(input)).isEqualTo(input)
    }

    @Test
    fun `Byte edge values roundtrip`() {
        val input = mapOf("key" to listOf(Byte.MIN_VALUE, 0.toByte(), Byte.MAX_VALUE))
        assertThat(roundtrip(input)).isEqualTo(input)
    }

    @Test
    fun `Short roundtrips`() {
        val input = mapOf("key" to listOf(256.toShort()))
        assertThat(roundtrip(input)).isEqualTo(input)
    }

    @Test
    fun `Short edge values roundtrip`() {
        val input = mapOf("key" to listOf(Short.MIN_VALUE, 0.toShort(), Short.MAX_VALUE))
        assertThat(roundtrip(input)).isEqualTo(input)
    }

    @Test
    fun `list with mixed primitive types roundtrips`() {
        val input =
            mapOf(
                "key" to
                    listOf(42, "text", true, 3.14f, 100L, 2.71, 'Z', 7.toByte(), 99.toShort(), null)
            )
        val result = roundtrip(input)
        assertThat(result).isEqualTo(input)
    }

    @Test
    fun `SnapshotMutableState with Int roundtrips`() {
        val input = mapOf("key" to listOf(mutableStateOf(42)))
        val result = roundtrip(input)

        val restored = result["key"]!!.single()
        assertThat(restored).isNotNull().isInstanceOf<SnapshotMutableState<*>>()
        assertThat((restored as SnapshotMutableState<*>).value).isEqualTo(42)
    }

    @Test
    fun `SnapshotMutableState with String roundtrips`() {
        val input = mapOf("key" to listOf(mutableStateOf("hello")))
        val result = roundtrip(input)

        val restored = result["key"]!!.single()
        assertThat(restored).isNotNull().isInstanceOf<SnapshotMutableState<*>>()
        assertThat((restored as SnapshotMutableState<*>).value).isEqualTo("hello")
    }

    @Test
    fun `SnapshotMutableState with null value roundtrips`() {
        val input = mapOf("key" to listOf(mutableStateOf<String?>(null)))
        val result = roundtrip(input)

        val restored = result["key"]!!.single()
        assertThat(restored).isNotNull().isInstanceOf<SnapshotMutableState<*>>()
        assertThat((restored as SnapshotMutableState<*>).value).isNull()
    }

    @Test
    fun `SnapshotMutableState with Boolean roundtrips`() {
        val input = mapOf("key" to listOf(mutableStateOf(true)))
        val result = roundtrip(input)

        val restored = result["key"]!!.single()
        assertThat(restored).isNotNull().isInstanceOf<SnapshotMutableState<*>>()
        assertThat((restored as SnapshotMutableState<*>).value).isEqualTo(true)
    }

    @Test
    fun `nested list of primitives roundtrips`() {
        val inner = listOf(1, 2, 3)
        val input = mapOf("key" to listOf(inner))
        val result = roundtrip(input)
        assertThat(result["key"]!!.single()).isEqualTo(inner)
    }

    @Test
    fun `empty nested list roundtrips`() {
        val input = mapOf("key" to listOf(emptyList<Any?>()))
        val result = roundtrip(input)
        assertThat(result["key"]!!.single()).isEqualTo(emptyList<Any?>())
    }

    @Test
    fun `nested list with mixed types roundtrips`() {
        val inner = listOf(1, "two", null, true)
        val input = mapOf("key" to listOf(inner))
        val result = roundtrip(input)
        assertThat(result["key"]!!.single()).isEqualTo(inner)
    }

    @Test
    fun `deeply nested list roundtrips`() {
        val innermost = listOf(1, 2)
        val middle = listOf(innermost, "other")
        val input = mapOf("key" to listOf(middle))
        val result = roundtrip(input)

        @Suppress("UNCHECKED_CAST") val restoredMiddle = result["key"]!!.single() as List<Any?>
        assertThat(restoredMiddle).hasSize(2)
        assertThat(restoredMiddle[0]).isEqualTo(innermost)
        assertThat(restoredMiddle[1]).isEqualTo("other")
    }

    @Test
    fun `nested map roundtrips`() {
        val inner = mapOf("a" to 1, "b" to 2)
        val input = mapOf("key" to listOf(inner))
        val result = roundtrip(input)
        assertThat(result["key"]!!.single()).isEqualTo(inner)
    }

    @Test
    fun `empty nested map roundtrips`() {
        val input = mapOf("key" to listOf(emptyMap<String, Any?>()))
        val result = roundtrip(input)
        assertThat(result["key"]!!.single()).isEqualTo(emptyMap<String, Any?>())
    }

    @Test
    fun `nested map with mixed value types roundtrips`() {
        val inner = mapOf("int" to 1, "str" to "hello", "null" to null, "bool" to true)
        val input = mapOf("key" to listOf(inner))
        val result = roundtrip(input)
        assertThat(result["key"]!!.single()).isEqualTo(inner)
    }

    @Test
    fun `map keys are converted to strings`() {
        val inner = mapOf(1 to "one", 2 to "two")
        val input = mapOf("key" to listOf(inner))
        val result = roundtrip(input)

        @Suppress("UNCHECKED_CAST") val restored = result["key"]!!.single() as Map<String, Any?>
        assertThat(restored).key("1").isEqualTo("one")
        assertThat(restored).key("2").isEqualTo("two")
    }

    @Test
    fun `SavedState with primitives roundtrips`() {
        val state = savedState(mapOf("x" to 10, "y" to "hello"))
        val input = mapOf("key" to listOf(state))
        val result = roundtrip(input)

        val restored = result["key"]!!.single()
        assertThat(restored).isNotNull().isInstanceOf<androidx.savedstate.SavedState>()
        (restored as androidx.savedstate.SavedState).read {
            assertThat(toMap()).isEqualTo(mapOf("x" to 10, "y" to "hello"))
        }
    }

    @Test
    fun `empty SavedState roundtrips`() {
        val state = savedState()
        val input = mapOf("key" to listOf(state))
        val result = roundtrip(input)

        val restored = result["key"]!!.single()
        assertThat(restored).isNotNull().isInstanceOf<androidx.savedstate.SavedState>()
        (restored as androidx.savedstate.SavedState).read {
            assertThat(toMap()).isEqualTo(emptyMap())
        }
    }

    @Test
    fun `complex nested structure roundtrips`() {
        val input =
            mapOf(
                "primitives" to listOf(1, "two", 3.0, null),
                "nested" to listOf(listOf(1, 2), mapOf("a" to "b")),
                "state" to listOf(mutableStateOf(42)),
            )
        val result = roundtrip(input)

        assertThat(result["primitives"]).isEqualTo(listOf(1, "two", 3.0, null))
        assertThat(result["nested"]!![0]).isEqualTo(listOf(1, 2))
        assertThat(result["nested"]!![1]).isEqualTo(mapOf("a" to "b"))

        val restoredState = result["state"]!!.single()
        assertThat(restoredState).isNotNull().isInstanceOf<SnapshotMutableState<*>>()
        assertThat((restoredState as SnapshotMutableState<*>).value).isEqualTo(42)
    }

    @Test
    fun `list containing map containing list roundtrips`() {
        val innerList = listOf(10, 20)
        val innerMap = mapOf("nums" to innerList)
        val input = mapOf("key" to listOf(listOf(innerMap)))
        val result = roundtrip(input)

        @Suppress("UNCHECKED_CAST") val outerList = result["key"]!!.single() as List<Any?>
        @Suppress("UNCHECKED_CAST") val restoredMap = outerList.single() as Map<String, Any?>
        assertThat(restoredMap["nums"]).isEqualTo(innerList)
    }

    @Test
    fun `unsupported type serializes as null`() {
        val unsupported = object {}
        val input = mapOf("key" to listOf(unsupported))
        val result = roundtrip(input)
        assertThat(result["key"]!!.single()).isNull()
    }

    @Test
    fun `serialized output is valid JSON`() {
        val input = mapOf("key" to listOf(42, "hello", null))
        val encoded = json.encodeToString(SavedStateSerializer, input)
        val reparsed = json.parseToJsonElement(encoded)
        assertThat(reparsed.toString()).isEqualTo(encoded)
    }

    @Test
    fun `roundtrip preserves all entry values across multiple keys`() {
        val input =
            mapOf(
                "ints" to listOf(1, 2, 3),
                "strings" to listOf("a", "b", "c"),
                "booleans" to listOf(true, false),
                "nulls" to listOf(null, null),
                "mixed" to listOf(1, "two", 3.0f, null, true),
            )
        val result = roundtrip(input)

        assertThat(result["ints"]).isEqualTo(listOf(1, 2, 3))
        assertThat(result["strings"]).isEqualTo(listOf("a", "b", "c"))
        assertThat(result["booleans"]).isEqualTo(listOf(true, false))
        assertThat(result["nulls"]).isEqualTo(listOf(null, null))
        assertThat(result["mixed"]).isEqualTo(listOf(1, "two", 3.0f, null, true))
    }

    @Test
    fun `double roundtrip produces identical result`() {
        val input = mapOf("key" to listOf(42, "hello", listOf(1, 2), mapOf("a" to true)))
        val first = roundtrip(input)
        val second = roundtrip(first)
        assertThat(second).isEqualTo(first)
    }

    @Test
    fun `SnapshotMutableState inside nested list roundtrips`() {
        val input = mapOf("key" to listOf(listOf(mutableStateOf(99))))
        val result = roundtrip(input)

        @Suppress("UNCHECKED_CAST") val innerList = result["key"]!!.single() as List<Any?>
        val restored = innerList.single()
        assertThat(restored).isNotNull().isInstanceOf<SnapshotMutableState<*>>()
        assertThat((restored as SnapshotMutableState<*>).value).isEqualTo(99)
    }

    @Test
    fun `String with unicode characters roundtrips`() {
        val input = mapOf("key" to listOf("Hello \uD83D\uDE00 World", "Привіт", "日本語"))
        assertThat(roundtrip(input)).isEqualTo(input)
    }

    @Test
    fun `String with special JSON characters roundtrips`() {
        val input = mapOf("key" to listOf("line1\nline2", "tab\there", "quote\"inside"))
        assertThat(roundtrip(input)).isEqualTo(input)
    }

    @Test
    fun `large list roundtrips`() {
        val largeList = (0 until 100).map { it }
        val input = mapOf("key" to largeList)
        assertThat(roundtrip(input)).isEqualTo(input)
    }

    @Test
    fun `multiple SnapshotMutableState values in one list roundtrip`() {
        val input =
            mapOf("key" to listOf(mutableStateOf(1), mutableStateOf("hello"), mutableStateOf(true)))
        val result = roundtrip(input)
        val list = result["key"]!!
        assertThat(list).hasSize(3)
        assertThat((list[0] as SnapshotMutableState<*>).value).isEqualTo(1)
        assertThat((list[1] as SnapshotMutableState<*>).value).isEqualTo("hello")
        assertThat((list[2] as SnapshotMutableState<*>).value).isEqualTo(true)
    }

    @Test
    fun `negative Float roundtrips`() {
        val input = mapOf("key" to listOf(-1.5f))
        assertThat(roundtrip(input)).isEqualTo(input)
    }

    @Test
    fun `negative Double roundtrips`() {
        val input = mapOf("key" to listOf(-2.718))
        assertThat(roundtrip(input)).isEqualTo(input)
    }

    @Test
    fun `negative Int roundtrips`() {
        val input = mapOf("key" to listOf(-42))
        assertThat(roundtrip(input)).isEqualTo(input)
    }

    @Test
    fun `negative Long roundtrips`() {
        val input = mapOf("key" to listOf(-999999999L))
        assertThat(roundtrip(input)).isEqualTo(input)
    }

    @Test
    fun `SavedState with null value roundtrips`() {
        val state = savedState(mapOf("nullable" to null))
        val input = mapOf("key" to listOf(state))
        val result = roundtrip(input)

        val restored = result["key"]!!.single() as androidx.savedstate.SavedState
        restored.read { assertThat(toMap()).isEqualTo(mapOf("nullable" to null)) }
    }

    @Test
    fun `list with both SavedState and primitives roundtrips`() {
        val state = savedState(mapOf("inner" to 42))
        val input = mapOf("key" to listOf(state, "text", 99))
        val result = roundtrip(input)

        val list = result["key"]!!
        assertThat(list).hasSize(3)
        assertThat(list[0]).isNotNull().isInstanceOf<androidx.savedstate.SavedState>()
        (list[0] as androidx.savedstate.SavedState).read {
            assertThat(toMap()).isEqualTo(mapOf("inner" to 42))
        }
        assertThat(list[1]).isEqualTo("text")
        assertThat(list[2]).isEqualTo(99)
    }
}
