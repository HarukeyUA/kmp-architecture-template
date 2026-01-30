package org.example.project.core.component.internal

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder

internal actual object PlatformSavedStateRegistryUtils {
    actual fun canBeSaved(value: Any): Boolean = false

    actual fun write(encoder: CompositeEncoder, descriptor: SerialDescriptor, value: Any): Boolean {
        return false
    }

    actual fun read(
        decoder: CompositeDecoder,
        descriptor: SerialDescriptor,
        index: Int,
        type: Byte
    ): Any? {
        return null
    }
}
