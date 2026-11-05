package org.example.project.core.component.internal

import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.io.Serializable
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder

internal actual object PlatformSavedStateRegistryUtils {
    // Detekt's type resolution does not see the reads inside this actual object.
    @Suppress("UnusedPrivateProperty") private const val TYPE_SERIALIZABLE: Byte = 101

    // Everything, as on iOS: a lazy layout keys each item's saveables by a type of its own that
    // nothing here writes, and a desktop has no Bundle to refuse a value at composition. A value
    // the serializer cannot write costs its whole saveable, not just itself.
    actual fun canBeSaved(value: Any): Boolean = true

    // Serializable on the outer class says nothing about what it holds, so the answer is a dry run.
    actual fun isWritable(value: Any): Boolean =
        value is Serializable && javaSerialize(value) != null

    actual fun write(encoder: CompositeEncoder, descriptor: SerialDescriptor, value: Any): Boolean {
        val bytes = (value as? Serializable)?.let(::javaSerialize) ?: return false
        encoder.encodeByteElement(descriptor, 0, TYPE_SERIALIZABLE)
        encoder.encodeSerializableElement(descriptor, 1, ByteArraySerializer(), bytes)
        return true
    }

    actual fun read(
        decoder: CompositeDecoder,
        descriptor: SerialDescriptor,
        index: Int,
        type: Byte,
    ): Any? =
        if (type == TYPE_SERIALIZABLE) {
            val bytes = decoder.decodeSerializableElement(descriptor, index, ByteArraySerializer())
            ByteArrayInputStream(bytes).use { bis ->
                ObjectInputStream(bis).use { ois -> ois.readObject() }
            }
        } else {
            null
        }
}
