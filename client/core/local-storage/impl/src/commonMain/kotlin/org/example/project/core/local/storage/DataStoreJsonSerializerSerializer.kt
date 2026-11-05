package org.example.project.core.local.storage

import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.json.okio.encodeToBufferedSink
import okio.BufferedSink
import okio.BufferedSource

@ExperimentalSerializationApi
internal fun <T> dataStoreJsonSerializer(
    serializer: KSerializer<T>,
    defaultValue: T,
    unreadableValue: T,
): OkioSerializer<T> {
    val json = Json { ignoreUnknownKeys = true }

    return object : OkioSerializer<T> {
        override val defaultValue: T = defaultValue

        override suspend fun readFrom(source: BufferedSource): T =
            try {
                json.decodeFromBufferedSource(serializer, source)
            } catch (_: SerializationException) {
                unreadableValue
            }

        override suspend fun writeTo(t: T, sink: BufferedSink) {
            json.encodeToBufferedSink(serializer, t, sink)
        }
    }
}
