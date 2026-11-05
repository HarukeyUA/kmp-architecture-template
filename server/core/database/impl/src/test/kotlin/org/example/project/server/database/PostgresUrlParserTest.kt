package org.example.project.server.database

import assertk.assertThat
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.test.assertFailsWith

class PostgresUrlParserTest {
    @Test
    fun `postgres URL is split into jdbc URL and credentials`() {
        val parts = PostgresUrl.parse("postgres://alice:secret@db.internal:5432/app")

        assertThat(parts.jdbcUrl).isEqualTo("jdbc:postgresql://db.internal:5432/app")
        assertThat(parts.user).isEqualTo("alice")
        assertThat(parts.password).isEqualTo("secret")
    }

    @Test
    fun `query string is kept and percent-encoded credentials are decoded`() {
        val parts = PostgresUrl.parse("postgresql://alice:p%40ss@db.internal/app?sslmode=require")

        assertThat(parts.jdbcUrl).isEqualTo("jdbc:postgresql://db.internal/app?sslmode=require")
        assertThat(parts.password).isEqualTo("p@ss")
    }

    @Test
    fun `already-jdbc URL is returned verbatim without credentials`() {
        val parts = PostgresUrl.parse("jdbc:postgresql://db.internal:5432/app?sslmode=require")

        assertThat(parts.jdbcUrl)
            .isEqualTo("jdbc:postgresql://db.internal:5432/app?sslmode=require")
        assertThat(parts.user).isNull()
        assertThat(parts.password).isNull()
    }

    @Test
    fun `a host the URI cannot parse is rejected rather than defaulted`() {
        assertFailsWith<IllegalArgumentException> {
            PostgresUrl.parse("postgres://alice:secret@my_db:5432/app")
        }
    }

    @Test
    fun `a foreign scheme is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            PostgresUrl.parse("mysql://alice:secret@db.internal/app")
        }
    }

    @Test
    fun `a malformed URL fails without echoing the password`() {
        val failure =
            assertFailsWith<IllegalArgumentException> {
                PostgresUrl.parse("postgres://alice:p%ss@db.internal/app")
            }

        assertThat(failure.message.orEmpty()).doesNotContain("p%ss")
    }
}
