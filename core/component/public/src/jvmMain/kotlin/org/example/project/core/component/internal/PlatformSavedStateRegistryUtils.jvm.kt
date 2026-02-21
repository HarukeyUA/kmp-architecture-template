package org.example.project.core.component.internal

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder

internal actual object PlatformSavedStateRegistryUtils {
    private const val TYPE_SERIALIZABLE: Byte = 101

    actual fun canBeSaved(value: Any): Boolean {
        return value is Serializable
    }

    actual fun write(encoder: CompositeEncoder, descriptor: SerialDescriptor, value: Any): Boolean =
        when (value) {
            is Serializable -> {
                val bytes =
                    ByteArrayOutputStream().use { bos ->
                        ObjectOutputStream(bos).use { oos -> oos.writeObject(value) }
                        bos.toByteArray()
                    }

                encoder.encodeByteElement(descriptor, 0, TYPE_SERIALIZABLE)
                encoder.encodeSerializableElement(descriptor, 1, ByteArraySerializer(), bytes)
                true
            }

            else -> false
        }

    actual fun read(
        decoder: CompositeDecoder,
        descriptor: SerialDescriptor,
        index: Int,
        type: Byte,
    ): Any? =
        when (type) {
            TYPE_SERIALIZABLE -> {
                val bytes =
                    decoder.decodeSerializableElement(descriptor, index, ByteArraySerializer())
                ByteArrayInputStream(bytes).use { bis ->
                    ObjectInputStream(bis).use { ois -> ois.readObject() }
                }
            }

            else -> null
        }
}
