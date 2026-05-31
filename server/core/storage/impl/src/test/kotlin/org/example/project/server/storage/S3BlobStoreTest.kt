package org.example.project.server.storage

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.createBucket
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.runBlocking
import org.testcontainers.containers.MinIOContainer

/**
 * The Phase 6 storage gate (ADR-0010): a blob round-trips through real object storage end-to-end. A
 * MinIO Testcontainer stands in for S3 (both speak the same path-style protocol), and the test
 * drives the **presigned-URL** transfer model the [BlobStore] exposes — the bytes go straight to
 * the store over HTTP, never through the app — proving the interface works against a real backend,
 * not a mock.
 */
class S3BlobStoreTest {
    private lateinit var minio: MinIOContainer
    private lateinit var client: S3Client
    private lateinit var store: BlobStore
    private lateinit var http: HttpClient

    @BeforeTest
    fun setUp(): Unit = runBlocking {
        minio = MinIOContainer("minio/minio:latest")
        minio.start()
        val config =
            StorageConfig(
                endpoint = minio.s3URL,
                region = "us-east-1",
                bucket = "test-bucket",
                accessKey = minio.userName,
                secretKey = minio.password,
            )
        client = buildS3Client(config)
        // MinIO doesn't auto-provision buckets; create it before the round-trip.
        client.createBucket { bucket = config.bucket }
        store = S3BlobStore(client, config)
        http = HttpClient(OkHttp)
    }

    @AfterTest
    fun tearDown() {
        client.close()
        http.close()
        minio.stop()
    }

    @Test
    fun `presigned PUT round-trips bytes a presigned GET reads back, then delete removes them`():
        Unit = runBlocking {
        val key = "objects/round-trip"
        val payload = ByteArray(256) { (it and 0xFF).toByte() }

        // Upload directly to the store via the signed PUT URL — bytes never touch the app.
        val putUrl = store.presignPut(key, payload.size.toLong(), 5.minutes)
        val put =
            http.put(putUrl) {
                headers { append(HttpHeaders.ContentLength, payload.size.toString()) }
                setBody(payload)
            }
        assertThat(put.status).isEqualTo(HttpStatusCode.OK)

        // head reports the stored size.
        assertThat(store.headSize(key)).isEqualTo(payload.size.toLong())

        // Download via the signed GET URL — same bytes back.
        val getUrl = store.presignGet(key, 5.minutes)
        val get = http.get(getUrl)
        assertThat(get.status).isEqualTo(HttpStatusCode.OK)
        assertThat(get.bodyAsBytes()).isEqualTo(payload)

        // delete removes it: head now reports absent and a fresh GET 404s.
        store.delete(key)
        assertThat(store.headSize(key)).isNull()
        assertThat(http.get(store.presignGet(key, 5.minutes)).status)
            .isEqualTo(HttpStatusCode.NotFound)
    }

    @Test
    fun `delete is idempotent against a missing key`(): Unit = runBlocking {
        // Must not throw even though nothing is stored at the key.
        store.delete("objects/never-existed")
    }

    @Test
    fun `the store rejects a PUT whose Content-Length differs from the signed length`(): Unit =
        runBlocking {
            val key = "objects/size-pinned"
            // Sign for 64 bytes, then try to upload 32 — the baked-in length must make S3 reject
            // it.
            val putUrl = store.presignPut(key, contentLengthBytes = 64, ttl = 5.minutes)
            val response =
                http.put(putUrl) {
                    headers { append(HttpHeaders.ContentLength, "32") }
                    setBody(ByteArray(32) { 0x42 })
                }
            assertThat(response.status.value).isBetween(400, 499)
        }
}
