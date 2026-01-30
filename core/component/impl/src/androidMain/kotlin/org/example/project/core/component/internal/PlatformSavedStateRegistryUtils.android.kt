package org.example.project.core.component.internal

import android.os.Parcel
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

internal actual object PlatformSavedStateRegistryUtils {
    private const val TYPE_PARCELABLE: Byte = 100
    private const val TYPE_SERIALIZABLE: Byte = 101

    actual fun canBeSaved(value: Any): Boolean {
        return value is Parcelable ||
                value is Serializable ||
                value is Size ||
                value is SizeF ||
                value is SparseArray<*>
    }

    actual fun write(encoder: CompositeEncoder, descriptor: SerialDescriptor, value: Any): Boolean =
        when (value) {
            is Parcelable -> {
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

            is Serializable -> {
                val bytes = ByteArrayOutputStream().use { bos ->
                    ObjectOutputStream(bos).use { oos ->
                        oos.writeObject(value)
                    }
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
        type: Byte
    ): Any? = when (type) {
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
                ObjectInputStream(bis).use { ois ->
                    ois.readObject()
                }
            }
        }

        else -> null
    }
}
