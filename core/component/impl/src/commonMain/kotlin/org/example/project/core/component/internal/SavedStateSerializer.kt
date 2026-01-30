package org.example.project.core.component.internal

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure

internal object SavedStateSerializer : KSerializer<Map<String, List<Any?>>> {
    override val descriptor: SerialDescriptor =
        MapSerializer(String.serializer(), AnyListSerializer).descriptor

    override fun serialize(encoder: Encoder, value: Map<String, List<Any?>>) {
        MapSerializer(String.serializer(), AnyListSerializer).serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): Map<String, List<Any?>> {
        return MapSerializer(String.serializer(), AnyListSerializer).deserialize(decoder)
    }
}

private object AnyListSerializer : KSerializer<List<Any?>> {
    override val descriptor: SerialDescriptor = ListSerializer(AnyValueSerializer).descriptor

    override fun serialize(encoder: Encoder, value: List<Any?>) {
        ListSerializer(AnyValueSerializer).serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): List<Any?> {
        return ListSerializer(AnyValueSerializer).deserialize(decoder)
    }
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
    SAVED_STATE(13)
}

private object AnyValueSerializer : KSerializer<Any?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("AnyValue") {
        element("type", Byte.serializer().descriptor)
        element("value", buildClassSerialDescriptor("Value"))
    }

    override fun serialize(encoder: Encoder, value: Any?) {
        val composite = encoder.beginStructure(descriptor)
        when (value) {
            null -> {
                composite.encodeByteElement(descriptor, 0, SerializableTypes.NULL.code)
            }

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
                    SerializableTypes.SNAPSHOT_MUTABLE_VALUE.code
                )
                composite.encodeSerializableElement(descriptor, 1, AnyValueSerializer, value.value)
            }

            is List<*> -> {
                composite.encodeByteElement(descriptor, 0, SerializableTypes.LIST.code)
                composite.encodeSerializableElement(
                    descriptor,
                    1,
                    AnyListSerializer,
                    value as List<Any?>
                )
            }

            is Map<*, *> -> {
                composite.encodeByteElement(descriptor, 0, SerializableTypes.MAP.code)
                composite.encodeSerializableElement(
                    descriptor,
                    1,
                    AnyMapSerializer,
                    value as Map<String, Any?>
                )
            }

            is SavedState -> {
                composite.encodeByteElement(descriptor, 0, SerializableTypes.SAVED_STATE.code)
                composite.encodeSerializableElement(
                    descriptor,
                    1,
                    AnyMapSerializer,
                    value.toMap()
                )
            }

            else -> {
                if (!PlatformSavedStateRegistryUtils.write(composite, descriptor, value)) {
                    throw SerializationException("Value of type ${value::class} is not supported by SavedStateSerializer")
                }
            }
        }
        composite.endStructure(descriptor)
    }

    private fun SavedState.toMap(): Map<String, Any?> = read { toMap() }

    override fun deserialize(decoder: Decoder): Any? {
        return decoder.decodeStructure(descriptor) {
            var type: Byte? = null
            var value: Any? = null

            while (true) {
                when (decodeElementIndex(descriptor)) {
                    0 -> type = decodeByteElement(descriptor, 0)
                    1 -> {
                        value = when (SerializableTypes.entries.firstOrNull { it.code == type }) {
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
                                decodeSerializableElement(
                                    descriptor,
                                    1,
                                    AnyListSerializer
                                )

                            SerializableTypes.MAP -> decodeSerializableElement(
                                descriptor,
                                1,
                                AnyMapSerializer
                            )

                            SerializableTypes.SAVED_STATE -> savedState(
                                decodeSerializableElement(
                                    descriptor,
                                    1,
                                    AnyMapSerializer
                                )
                            )

                            null -> PlatformSavedStateRegistryUtils.read(
                                this,
                                descriptor,
                                1,
                                type!!
                            ) ?: throw SerializationException("Unknown type tag: $type")
                        }
                    }

                    else -> break
                }
            }
            value
        }
    }
}

private object AnyMapSerializer : KSerializer<Map<String, Any?>> {
    override val descriptor: SerialDescriptor =
        MapSerializer(String.serializer(), AnyValueSerializer).descriptor

    override fun serialize(encoder: Encoder, value: Map<String, Any?>) {
        MapSerializer(String.serializer(), AnyValueSerializer).serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): Map<String, Any?> {
        return MapSerializer(String.serializer(), AnyValueSerializer).deserialize(decoder)
    }
}
