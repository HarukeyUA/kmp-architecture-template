package org.example.project.core.component.internal

import arrow.core.raise.catch
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamException
import java.io.Serializable

// Kept identical in androidMain and jvmMain: the module has no source set shared by only those two.
/**
 * Java-serialized bytes of [value], or null when the stream refuses somewhere under it: a field
 * that is not Serializable, a class descriptor Java will not write. The caller logs the drop.
 */
internal fun javaSerialize(value: Serializable): ByteArray? =
    catch(
        {
            ByteArrayOutputStream().use { bos ->
                ObjectOutputStream(bos).use { oos -> oos.writeObject(value) }
                bos.toByteArray()
            }
        },
    ) { _: ObjectStreamException ->
        null
    }
