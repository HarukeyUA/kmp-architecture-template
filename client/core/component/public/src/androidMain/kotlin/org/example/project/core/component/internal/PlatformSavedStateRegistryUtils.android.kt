package org.example.project.core.component.internal

import android.os.Parcel
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.io.Serializable
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder

internal actual object PlatformSavedStateRegistryUtils {
    private const val TYPE_PARCELABLE: Byte = 100
    private const val TYPE_SERIALIZABLE: Byte = 101

    // As strict as the Bundle this used to be: what it refuses is refused at composition.
    actual fun canBeSaved(value: Any): Boolean = isWritable(value)

    // Serializable on the outer class says nothing about what it holds, so the answer is a dry run.
    actual fun isWritable(value: Any): Boolean =
        isParcelValue(value) || (value is Serializable && javaSerialize(value) != null)

    private fun isParcelValue(value: Any): Boolean =
        value is Parcelable || value is Size || value is SizeF || value is SparseArray<*>

    actual fun write(encoder: CompositeEncoder, descriptor: SerialDescriptor, value: Any): Boolean =
        when {
            isParcelValue(value) -> {
                val parcel = Parcel.obtain()
                try {
                    parcel.writeValue(value)
                    val bytes = parcel.marshall()

                    encoder.encodeByteElement(descriptor, 0, TYPE_PARCELABLE)
                    encoder.encodeSerializableElement(descriptor, 1, ByteArraySerializer(), bytes)
                    true
                } finally {
                    parcel.recycle()
                }
            }

            value is Serializable -> {
                val bytes = javaSerialize(value) ?: return false
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
            TYPE_PARCELABLE -> {
                val bytes =
                    decoder.decodeSerializableElement(descriptor, index, ByteArraySerializer())
                val parcel = Parcel.obtain()
                try {
                    parcel.unmarshall(bytes, 0, bytes.size)
                    parcel.setDataPosition(0)
                    parcel.readValue(PlatformSavedStateRegistryUtils::class.java.classLoader)
                } finally {
                    parcel.recycle()
                }
            }

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
