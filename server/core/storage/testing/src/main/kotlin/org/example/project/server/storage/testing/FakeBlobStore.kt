package org.example.project.server.storage.testing

import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import org.example.project.server.storage.BlobStore

/**
 * In-memory [BlobStore]. The presign methods return synthetic URLs embedding the key and the
 * reserved length. [put] stands in for the client's out-of-band PUT, and [headSize] reflects only
 * what has been [put], so confirm-time HEAD verification is exercised.
 */
class FakeBlobStore : BlobStore {
    private val objects = ConcurrentHashMap<String, ByteArray>()

    /** Keys actually removed, in removal order (a refused key is not recorded). */
    val deleted = mutableListOf<String>()

    /** When non-null, the whole delete request fails: [delete] and [deleteAll] both throw it. */
    var deleteFailure: Exception? = null

    /**
     * Keys the store refuses one at a time: [delete] throws, [deleteAll] returns them unremoved.
     */
    var refusedKeys: Set<String> = emptySet()

    override suspend fun presignPut(key: String, contentLengthBytes: Long, ttl: Duration): String =
        "https://fake-bucket/$key?contentLength=$contentLengthBytes"

    override suspend fun presignGet(key: String, ttl: Duration): String =
        "https://fake-bucket/$key?download"

    override suspend fun headSize(key: String): Long? = objects[key]?.size?.toLong()

    override suspend fun delete(key: String) {
        deleteFailure?.let { throw it }
        if (key in refusedKeys) throw BlobRefusedException(key)
        objects.remove(key)
        deleted += key
    }

    override suspend fun deleteAll(keys: Collection<String>): List<String> {
        deleteFailure?.let { throw it }
        val (refused, removable) = keys.partition { it in refusedKeys }
        removable.forEach {
            objects.remove(it)
            deleted += it
        }
        return refused
    }

    fun put(key: String, bytes: ByteArray) {
        objects[key] = bytes
    }

    fun get(key: String): ByteArray? = objects[key]
}

/** What a [FakeBlobStore.refusedKeys] entry throws out of a single-key delete. */
class BlobRefusedException(key: String) : RuntimeException("Blob store refused $key")
