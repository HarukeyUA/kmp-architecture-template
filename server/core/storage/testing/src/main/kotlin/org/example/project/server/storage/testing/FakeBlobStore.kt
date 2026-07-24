package org.example.project.server.storage.testing

import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import org.example.project.server.storage.BlobStore

/**
 * In-memory [BlobStore] for service unit tests in a domain that owns blobs (an avatar feature,
 * say). [presignPut]/[presignGet] return synthetic URLs embedding the key and (for PUT) the
 * reserved length, so a test can assert on them without a real store. [put] simulates the client's
 * out-of-band PUT; [headSize] reflects only what has been [put], so confirm-time HEAD verification
 * is exercised. [deleteFailure], when set, makes [delete] throw — the "bucket fails, DB row
 * preserved" path.
 */
class FakeBlobStore : BlobStore {
    private val objects = ConcurrentHashMap<String, ByteArray>()

    /** Keys passed to [delete] that were actually removed (a failing delete is not recorded). */
    val deleted = mutableListOf<String>()

    /** When non-null, [delete] throws it instead of removing the object. */
    var deleteFailure: Exception? = null

    override suspend fun presignPut(key: String, contentLengthBytes: Long, ttl: Duration): String =
        "https://fake-bucket/$key?contentLength=$contentLengthBytes"

    override suspend fun presignGet(key: String, ttl: Duration): String =
        "https://fake-bucket/$key?download"

    override suspend fun headSize(key: String): Long? = objects[key]?.size?.toLong()

    override suspend fun delete(key: String) {
        deleteFailure?.let { throw it }
        objects.remove(key)
        deleted += key
    }

    /** Simulates the client PUT of [bytes] to [key]. */
    fun put(key: String, bytes: ByteArray) {
        objects[key] = bytes
    }

    fun get(key: String): ByteArray? = objects[key]
}
