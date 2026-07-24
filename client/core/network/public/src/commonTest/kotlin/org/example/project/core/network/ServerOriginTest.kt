package org.example.project.core.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

/**
 * Pins the structural checks [normalizeOriginOrNull] layers on top of Ktor's lenient `Url` parser:
 * the traps below (free text "parsing" against a localhost fallback, `localhost:8080` read as a
 * scheme) would otherwise pass silently and admit garbage as a dev origin.
 */
class ServerOriginTest {
    @Test
    fun `canonicalizes a valid origin with explicit port and lowercased host`() {
        assertThat(normalizeOriginOrNull("http://Polinas-Mac.local:8080"))
            .isEqualTo("http://polinas-mac.local:8080")
    }

    @Test
    fun `makes elided default ports explicit`() {
        assertThat(normalizeOriginOrNull("https://api.example.com"))
            .isEqualTo("https://api.example.com:443")
        assertThat(normalizeOriginOrNull("http://api.example.com/"))
            .isEqualTo("http://api.example.com:80")
    }

    @Test
    fun `rejects anything that is not a bare absolute http origin`() {
        // Ktor "parses" free text against a localhost fallback — parse success is not validity.
        assertThat(normalizeOriginOrNull("not a url")).isNull()
        // Ktor treats `localhost:8080` as a URL with scheme `localhost`.
        assertThat(normalizeOriginOrNull("localhost:8080")).isNull()
        assertThat(normalizeOriginOrNull("ftp://example.com")).isNull()
        assertThat(normalizeOriginOrNull("")).isNull()
    }

    @Test
    fun `rejects path query and fragment suffixes`() {
        assertThat(normalizeOriginOrNull("http://example.com:8080/api")).isNull()
        assertThat(normalizeOriginOrNull("http://example.com?x=1")).isNull()
        assertThat(normalizeOriginOrNull("http://example.com#frag")).isNull()
    }
}
