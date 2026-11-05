package org.example.project.core.component.internal

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState
import co.touchlab.kermit.Logger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.PairSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure

private val log = Logger.withTag("SavedState")

internal object SavedStateSerializer : KSerializer<Map<String, List<Any?>>> {
    private val delegate = MapSerializer(String.serializer(), AnyListSerializer)

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: Map<String, List<Any?>>) {
        delegate.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): Map<String, List<Any?>> =
        delegate.deserialize(decoder)
}

private object AnyListSerializer : KSerializer<List<Any?>> {
    private val delegate = ListSerializer(AnyValueSerializer)

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: List<Any?>) {
        delegate.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): List<Any?> = delegate.deserialize(decoder)
}

private enum class SerializableTypes(val code: Byte) {
    NULL(0),
    INT(1),
    STRING(2),
    BOOLEAN(3),
    FLOAT(4),
    LONG(5),
    DOUBLE(6),
    CHAR(7),
    BYTE(8),
    SHORT(9),
    SNAPSHOT_MUTABLE_VALUE(10),
    LIST(11),
    MAP(12),
    SAVED_STATE(13),
}

private object AnyValueSerializer : KSerializer<Any?> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("AnyValue") {
            element("type", Byte.serializer().descriptor)
            element("value", buildClassSerialDescriptor("Value"))
        }

    override fun serialize(encoder: Encoder, value: Any?) {
        val composite = encoder.beginStructure(descriptor)
        val unwritable = firstUnwritable(value)
        // Whole, or not at all: a saveable that comes back with a hole inside it (a state holding
        // null, a list missing an element) is worse than one that comes back empty. Maps are the
        // exception, entry by entry, so one odd item cannot take a holder down.
        when {
            value == null -> composite.encodeNullTag()
            unwritable == null -> writeValue(composite, value)
            else -> {
                log.w {
                    "Dropped a saveable of ${value::class}: nothing here writes the " +
                        "${unwritable::class} inside it"
                }
                composite.encodeNullTag()
            }
        }
        composite.endStructure(descriptor)
    }

    private fun CompositeEncoder.encodeNullTag() {
        encodeByteElement(descriptor, 0, SerializableTypes.NULL.code)
    }

    private fun writeValue(composite: CompositeEncoder, value: Any) {
        when (value) {
            is Int -> {
                composite.encodeByteElement(descriptor, 0, SerializableTypes.INT.code)
                composite.encodeIntElement(descriptor, 1, value)
            }

            is String -> {
                composite.encodeByteElement(descriptor, 0, SerializableTypes.STRING.code)
                composite.encodeStringElement(descriptor, 1, value)
            }

            is Boolean -> {
                composite.encodeByteElement(descriptor, 0, SerializableTypes.BOOLEAN.code)
                composite.encodeBooleanElement(descriptor, 1, value)
            }

            is Float -> {
                composite.encodeByteElement(descriptor, 0, SerializableTypes.FLOAT.code)
                composite.encodeFloatElement(descriptor, 1, value)
            }

            is Long -> {
                composite.encodeByteElement(descriptor, 0, SerializableTypes.LONG.code)
                composite.encodeLongElement(descriptor, 1, value)
            }

            is Double -> {
                composite.encodeByteElement(descriptor, 0, SerializableTypes.DOUBLE.code)
                composite.encodeDoubleElement(descriptor, 1, value)
            }

            is Char -> {
                composite.encodeByteElement(descriptor, 0, SerializableTypes.CHAR.code)
                composite.encodeCharElement(descriptor, 1, value)
            }

            is Byte -> {
                composite.encodeByteElement(descriptor, 0, SerializableTypes.BYTE.code)
                composite.encodeByteElement(descriptor, 1, value)
            }

            is Short -> {
                composite.encodeByteElement(descriptor, 0, SerializableTypes.SHORT.code)
                composite.encodeShortElement(descriptor, 1, value)
            }

            is SnapshotMutableState<*> -> {
                composite.encodeByteElement(
                    descriptor,
                    0,
                    SerializableTypes.SNAPSHOT_MUTABLE_VALUE.code,
                )
                composite.encodeSerializableElement(descriptor, 1, AnyValueSerializer, value.value)
            }

            is List<*> -> {
                composite.encodeByteElement(descriptor, 0, SerializableTypes.LIST.code)
                composite.encodeSerializableElement(descriptor, 1, AnyListSerializer, value)
            }

            is Map<*, *> -> {
                composite.encodeByteElement(descriptor, 0, SerializableTypes.MAP.code)
                composite.encodeSerializableElement(descriptor, 1, MapEntriesSerializer, value)
            }

            is SavedState -> {
                composite.encodeByteElement(descriptor, 0, SerializableTypes.SAVED_STATE.code)
                composite.encodeSerializableElement(descriptor, 1, AnyMapSerializer, value.toMap())
            }

            // Unreachable after firstUnwritable; kept so a platform that disagrees still degrades.
            else -> {
                if (!PlatformSavedStateRegistryUtils.write(composite, descriptor, value)) {
                    composite.encodeNullTag()
                }
            }
        }
    }

    private fun SavedState.toMap(): Map<String, Any?> = read { toMap() }

    /**
     * The innermost thing under [value] that nothing writes, or null when it all does. A map
     * answers null regardless: [MapEntriesSerializer] asks for each entry on its own.
     */
    fun firstUnwritable(value: Any?): Any? =
        when (value) {
            null,
            is Int,
            is String,
            is Boolean,
            is Long,
            is Char,
            is Byte,
            is Short -> null
            // JSON has no spelling for these.
            is Float -> value.takeUnless { it.isFinite() }
            is Double -> value.takeUnless { it.isFinite() }
            is SnapshotMutableState<*> -> firstUnwritable(value.value)
            is List<*> -> value.firstNotNullOfOrNull(::firstUnwritable)
            is Map<*, *> -> null
            is SavedState -> value.toMap().values.firstNotNullOfOrNull(::firstUnwritable)
            else -> value.takeUnless(PlatformSavedStateRegistryUtils::isWritable)
        }

    override fun deserialize(decoder: Decoder): Any? {
        return decoder.decodeStructure(descriptor) {
            var type: Byte? = null
            var value: Any? = null

            while (true) {
                when (decodeElementIndex(descriptor)) {
                    0 -> type = decodeByteElement(descriptor, 0)
                    1 -> {
                        value =
                            when (SerializableTypes.entries.firstOrNull { it.code == type }) {
                                SerializableTypes.NULL -> null
                                SerializableTypes.INT -> decodeIntElement(descriptor, 1)
                                SerializableTypes.STRING -> decodeStringElement(descriptor, 1)
                                SerializableTypes.BOOLEAN -> decodeBooleanElement(descriptor, 1)
                                SerializableTypes.FLOAT -> decodeFloatElement(descriptor, 1)
                                SerializableTypes.LONG -> decodeLongElement(descriptor, 1)
                                SerializableTypes.DOUBLE -> decodeDoubleElement(descriptor, 1)
                                SerializableTypes.CHAR -> decodeCharElement(descriptor, 1)
                                SerializableTypes.BYTE -> decodeByteElement(descriptor, 1)
                                SerializableTypes.SHORT -> decodeShortElement(descriptor, 1)
                                SerializableTypes.SNAPSHOT_MUTABLE_VALUE -> {
                                    val restoredValue =
                                        decodeSerializableElement(descriptor, 1, AnyValueSerializer)
                                    mutableStateOf(restoredValue)
                                }

                                SerializableTypes.LIST ->
                                    decodeSerializableElement(descriptor, 1, AnyListSerializer)

                                SerializableTypes.MAP ->
                                    decodeSerializableElement(descriptor, 1, MapEntriesSerializer)

                                SerializableTypes.SAVED_STATE ->
                                    savedState(
                                        decodeSerializableElement(descriptor, 1, AnyMapSerializer),
                                    )

                                null -> readPlatformValue(type)
                            }
                    }

                    else -> break
                }
            }
            value
        }
    }

    private fun CompositeDecoder.readPlatformValue(type: Byte?): Any {
        val tag = type ?: throw SerializationException("Missing type tag")
        return PlatformSavedStateRegistryUtils.read(this, descriptor, 1, tag)
            ?: throw SerializationException("Unknown type tag: $tag")
    }
}

private object AnyMapSerializer : KSerializer<Map<String, Any?>> {
    private val delegate = MapSerializer(String.serializer(), AnyValueSerializer)

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: Map<String, Any?>) {
        delegate.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): Map<String, Any?> = delegate.deserialize(decoder)
}

/**
 * A map as a list of typed pairs, so a lazy item's or a screen's key comes back as the object the
 * holder will look it up by. An entry is skipped whole when either its key or its value is
 * unwritable.
 */
private object MapEntriesSerializer : KSerializer<Map<*, *>> {
    private val delegate = ListSerializer(PairSerializer(AnyValueSerializer, AnyValueSerializer))

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: Map<*, *>) {
        val entries =
            value.entries.mapNotNull { (k, v) ->
                val unwritable =
                    AnyValueSerializer.firstUnwritable(k) ?: AnyValueSerializer.firstUnwritable(v)
                if (unwritable == null) {
                    k to v
                } else {
                    val keyType = if (k == null) "null" else k::class.toString()
                    log.w {
                        "Dropped a map entry keyed by $keyType: nothing here writes the " +
                            "${unwritable::class} in it"
                    }
                    null
                }
            }
        delegate.serialize(encoder, entries)
    }

    // Mutable on purpose: Compose's state holders take the map they saved back as a MutableMap.
    override fun deserialize(decoder: Decoder): Map<*, *> =
        delegate.deserialize(decoder).toMap(LinkedHashMap())
}
