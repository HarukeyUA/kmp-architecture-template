package org.example.project.core.component.internal

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder

internal expect object PlatformSavedStateRegistryUtils {
    /**
     * Whether a composition may hand the registry [value] at all; wider than [isWritable] off
     * Android.
     */
    fun canBeSaved(value: Any): Boolean

    /**
     * Whether [write] can put [value] in a dump; the answer is final, [write] does not fail after
     * it.
     */
    fun isWritable(value: Any): Boolean

    /**
     * Writes a platform type tag at element 0 and [value]'s bytes at element 1; false when not its
     * own.
     */
    fun write(encoder: CompositeEncoder, descriptor: SerialDescriptor, value: Any): Boolean

    /** The value behind [type], or null when no platform type carries that tag. */
    fun read(decoder: CompositeDecoder, descriptor: SerialDescriptor, index: Int, type: Byte): Any?
}
