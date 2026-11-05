package org.example.project.server.testing

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

/**
 * Single-node RustFS, the S3-compatible store the server tests run against (same image as
 * `docker-compose.yml`). Buckets are not pre-provisioned; callers create theirs.
 */
class RustFsContainer : GenericContainer<RustFsContainer>(IMAGE) {
    val accessKey = "test-access-key"
    val secretKey = "test-secret-key"

    init {
        withExposedPorts(S3_PORT)
        withEnv("RUSTFS_ACCESS_KEY", accessKey)
        withEnv("RUSTFS_SECRET_KEY", secretKey)
        // Empty sends logs to stdout instead of files under /logs inside the container.
        withEnv("RUSTFS_OBS_LOG_DIRECTORY", "")
        waitingFor(Wait.forHttp("/health/ready").forPort(S3_PORT))
    }

    val s3Url: String
        get() = "http://$host:${getMappedPort(S3_PORT)}"

    private companion object {
        const val IMAGE = "rustfs/rustfs:1.0.4"
        const val S3_PORT = 9000
    }
}
