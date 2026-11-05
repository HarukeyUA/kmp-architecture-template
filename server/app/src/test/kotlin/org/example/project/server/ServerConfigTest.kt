package org.example.project.server

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.minutes

/**
 * [ServerConfig.load] against a fake environment. The contract under test is "fail fast with a
 * clear message" with no environment gate: every rejection names the offending key, and
 * present-but-blank values behave like absent ones.
 */
class ServerConfigTest {
    private val requiredEnv =
        mapOf(
            "DATABASE_URL" to "postgres://alice:secret@db.internal:5432/app",
            "JWT_SECRET" to "0123456789abcdef0123456789abcdef",
        )

    private fun load(env: Map<String, String>): ServerConfig = ServerConfig.load { env[it] }

    @Test
    fun `required values alone yield defaults for the rest`() {
        val config = load(requiredEnv)

        assertThat(config.port).isEqualTo(8080)
        assertThat(config.database.maxPoolSize).isEqualTo(10)
        assertThat(config.metrics.port).isEqualTo(8081)
        assertThat(config.webLimits.maxRequestBodyBytes).isEqualTo(1_048_576L)
        assertThat(config.webLimits.credentialRateLimit).isEqualTo(10)
        assertThat(config.jwt.accessTokenTtl).isEqualTo(15.minutes)
        assertThat(config.jwt.issuer).isEqualTo("kmp-template")
    }

    @Test
    fun `postgres url is split into jdbc url and credentials`() {
        val config = load(requiredEnv)

        assertThat(config.database.jdbcUrl).isEqualTo("jdbc:postgresql://db.internal:5432/app")
        assertThat(config.database.username).isEqualTo("alice")
        assertThat(config.database.password).isEqualTo("secret")
    }

    @Test
    fun `jdbc url takes separately supplied credentials`() {
        val config =
            load(
                requiredEnv +
                    mapOf(
                        "DATABASE_URL" to "jdbc:postgresql://db.internal:5432/app",
                        "DATABASE_USER" to "bob",
                        "DATABASE_PASSWORD" to "hunter2",
                    ),
            )

        assertThat(config.database.jdbcUrl).isEqualTo("jdbc:postgresql://db.internal:5432/app")
        assertThat(config.database.username).isEqualTo("bob")
        assertThat(config.database.password).isEqualTo("hunter2")
    }

    @Test
    fun `jdbc url alone leaves the credentials to the url`() {
        val config =
            load(requiredEnv + ("DATABASE_URL" to "jdbc:postgresql://db/app?user=bob&password=x"))

        assertThat(config.database.username).isNull()
        assertThat(config.database.password).isNull()
    }

    @Test
    fun `loading never reads object storage values`() {
        val keysRead = mutableListOf<String>()

        ServerConfig.load { key ->
            keysRead += key
            requiredEnv[key]
        }

        assertThat(keysRead.filter { it.startsWith("S3_") }).isEmpty()
    }

    @Test
    fun `dropping any required value fails naming the key`() {
        requiredEnv.keys.forEach { key ->
            val failure = assertFailsWith<IllegalStateException> { load(requiredEnv - key) }
            assertThat(failure.message.orEmpty()).contains(key)
        }
    }

    @Test
    fun `blank required value fails like a missing one`() {
        val failure =
            assertFailsWith<IllegalStateException> { load(requiredEnv + ("JWT_SECRET" to " ")) }

        assertThat(failure.message.orEmpty()).contains("JWT_SECRET")
    }

    @Test
    fun `short jwt secret fails naming the key`() {
        val failure =
            assertFailsWith<IllegalStateException> {
                load(requiredEnv + ("JWT_SECRET" to "too-short"))
            }

        assertThat(failure.message.orEmpty()).contains("JWT_SECRET")
    }

    @Test
    fun `blank optional value falls back to the default instead of failing to parse`() {
        val config =
            load(requiredEnv + mapOf("SERVER_PORT" to "", "DATABASE_MAX_POOL_SIZE" to "  "))

        assertThat(config.port).isEqualTo(8080)
        assertThat(config.database.maxPoolSize).isEqualTo(10)
    }

    @Test
    fun `non-numeric value fails naming the offending key`() {
        val failure =
            assertFailsWith<IllegalStateException> { load(requiredEnv + ("SERVER_PORT" to "abc")) }

        assertThat(failure.message.orEmpty()).contains("SERVER_PORT")
        assertThat(failure.message.orEmpty()).contains("abc")
    }

    @Test
    fun `non-numeric long value fails naming the offending key`() {
        val failure =
            assertFailsWith<IllegalStateException> {
                load(requiredEnv + ("MAX_REQUEST_BODY_BYTES" to "1MiB"))
            }

        assertThat(failure.message.orEmpty()).contains("MAX_REQUEST_BODY_BYTES")
    }

    @Test
    fun `blank client ip header means socket address, not a blank header name`() {
        val config = load(requiredEnv + ("CLIENT_IP_HEADER" to ""))

        assertThat(config.webLimits.clientIpHeader).isNull()
    }

    @Test
    fun `non-numeric access token ttl fails naming the offending key`() {
        val failure =
            assertFailsWith<IllegalStateException> {
                load(requiredEnv + ("JWT_ACCESS_TTL_MINUTES" to "soon"))
            }

        assertThat(failure.message.orEmpty()).contains("JWT_ACCESS_TTL_MINUTES")
    }
}
