package org.example.project.server.storage

import kotlin.time.Duration

/**
 * Object storage behind one interface — **never local disk**. Backed by any S3-compatible store
 * (RustFS locally, Railway Buckets / AWS S3 in prod); going multi-node is then a config change, not
 * a rewrite, because no blob ever lives on an instance's filesystem.
 *
 * The transfer model is **presigned URLs**: the server signs a short-lived URL and the client
 * `PUT`s/`GET`s the bytes **directly** to object storage, so blob bytes never stream through the
 * app and the app stays stateless and memory-flat regardless of blob size. A domain that owns blobs
 * (e.g. an avatar feature) injects this, persists the [key], and hands the signed URL to its
 * client.
 *
 * [key] is the opaque object key the owning domain chooses (a UUID, a content hash, `"avatars/$id"`
 * …). This infra primitive is deliberately ignorant of what a key means.
 */
interface BlobStore {
    /**
     * A URL the client may `PUT` exactly [contentLengthBytes] bytes to, valid for [ttl]. The length
     * is baked into the signature, so a `PUT` whose actual `Content-Length` differs is rejected by
     * the store — the size limit is enforced by object storage, not trusted from the client.
     */
    suspend fun presignPut(key: String, contentLengthBytes: Long, ttl: Duration): String

    /** A URL the client may `GET` the object from, valid for [ttl]. */
    suspend fun presignGet(key: String, ttl: Duration): String

    /** The stored object's size in bytes, or `null` if no object exists at [key]. */
    suspend fun headSize(key: String): Long?

    /**
     * Removes the object at [key]. Idempotent: deleting a missing key is a no-op, never an error.
     */
    suspend fun delete(key: String)

    /**
     * Removes every object in [keys], batching the round-trips. Idempotent per key like [delete].
     * Returns the keys the store refused, so a caller tracking outstanding work retries only those;
     * a throw means the whole request failed and no key can be assumed gone.
     */
    suspend fun deleteAll(keys: Collection<String>): List<String>
}
