package org.example.project.core.component.internal

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder

internal expect object PlatformSavedStateRegistryUtils {
    fun canBeSaved(value: Any): Boolean

    fun write(encoder: CompositeEncoder, descriptor: SerialDescriptor, value: Any): Boolean

    fun read(decoder: CompositeDecoder, descriptor: SerialDescriptor, index: Int, type: Byte): Any?
}
