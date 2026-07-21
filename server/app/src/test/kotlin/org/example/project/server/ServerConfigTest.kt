package org.example.project.server

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.minutes

/**
 * [ServerConfig.load] against a fake environment — the `getenv` parameter exists exactly for this.
 * The contract under test is "fail fast with a clear message": every rejection must name the
 * offending key, and present-but-blank values behave like absent ones.
 */
class ServerConfigTest {

    private fun load(vararg env: Pair<String, String>): ServerConfig = ServerConfig.load { key ->
        env.toMap()[key]
    }

    @Test
    fun `empty environment yields dev defaults`() {
        val config = load()

        assertThat(config.port).isEqualTo(8080)
        assertThat(config.database.maxPoolSize).isEqualTo(10)
        assertThat(config.metrics.port).isEqualTo(8081)
        assertThat(config.webLimits.maxRequestBodyBytes).isEqualTo(1_048_576L)
        assertThat(config.webLimits.credentialRateLimit).isEqualTo(10)
        assertThat(config.jwt.accessTokenTtl).isEqualTo(15.minutes)
        assertThat(config.jwt.issuer).isEqualTo("kmp-template")
    }

    @Test
    fun `blank optional value falls back to the default instead of failing to parse`() {
        val config = load("SERVER_PORT" to "", "DATABASE_MAX_POOL_SIZE" to "  ")

        assertThat(config.port).isEqualTo(8080)
        assertThat(config.database.maxPoolSize).isEqualTo(10)
    }

    @Test
    fun `non-numeric value fails naming the offending key`() {
        val failure = assertFailsWith<IllegalStateException> { load("SERVER_PORT" to "abc") }

        assertThat(failure.message.orEmpty()).contains("SERVER_PORT")
        assertThat(failure.message.orEmpty()).contains("abc")
    }

    @Test
    fun `non-numeric long value fails naming the offending key`() {
        val failure =
            assertFailsWith<IllegalStateException> { load("MAX_REQUEST_BODY_BYTES" to "1MiB") }

        assertThat(failure.message.orEmpty()).contains("MAX_REQUEST_BODY_BYTES")
    }

    @Test
    fun `missing required value in production fails naming the offending key`() {
        val failure = assertFailsWith<IllegalStateException> { load("APP_ENV" to "production") }

        assertThat(failure.message.orEmpty()).contains("DATABASE_URL")
    }

    @Test
    fun `blank required value in production fails like a missing one`() {
        val failure =
            assertFailsWith<IllegalStateException> {
                load(
                    "APP_ENV" to "production",
                    "DATABASE_URL" to "jdbc:postgresql://db:5432/app",
                    "DATABASE_USER" to "app",
                    "DATABASE_PASSWORD" to "",
                )
            }

        assertThat(failure.message.orEmpty()).contains("DATABASE_PASSWORD")
    }

    @Test
    fun `blank client ip header means socket address, not a blank header name`() {
        val config = load("CLIENT_IP_HEADER" to "")

        assertThat(config.webLimits.clientIpHeader).isNull()
    }

    @Test
    fun `missing jwt secret in production fails naming the offending key`() {
        val failure = assertFailsWith<IllegalStateException> { load(*productionEnvWithoutJwt()) }

        assertThat(failure.message.orEmpty()).contains("JWT_SECRET")
    }

    @Test
    fun `short jwt secret in production fails naming the offending key`() {
        val failure =
            assertFailsWith<IllegalStateException> {
                load(*productionEnvWithoutJwt(), "JWT_SECRET" to "too-short")
            }

        assertThat(failure.message.orEmpty()).contains("JWT_SECRET")
    }

    @Test
    fun `non-numeric access token ttl fails naming the offending key`() {
        val failure =
            assertFailsWith<IllegalStateException> { load("JWT_ACCESS_TTL_MINUTES" to "soon") }

        assertThat(failure.message.orEmpty()).contains("JWT_ACCESS_TTL_MINUTES")
    }

    /** Every production-required key except the JWT secret, so its check is the one that trips. */
    private fun productionEnvWithoutJwt(): Array<Pair<String, String>> =
        arrayOf(
            "APP_ENV" to "production",
            "DATABASE_URL" to "jdbc:postgresql://db:5432/app",
            "DATABASE_USER" to "app",
            "DATABASE_PASSWORD" to "secret",
            "S3_ENDPOINT" to "https://storage.example.com",
            "S3_BUCKET" to "blobs",
            "S3_ACCESS_KEY" to "key",
            "S3_SECRET_KEY" to "secret",
        )
}
