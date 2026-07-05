package org.example.project.shared.auth

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer

/**
 * Golden-set freeze for the auth domain's `@SerialName`s (ADR-0005), sibling to the cross-cutting
 * freeze in `:shared:common`. These `auth.*` codes are the wire contract for un-updatable clients —
 * renaming one is a breaking change.
 */
@OptIn(ExperimentalSerializationApi::class)
class AuthErrorGoldenTest {
    @Test
    fun `auth error codes are frozen`() {
        assertThat(serializer<EmailTaken>().descriptor.serialName).isEqualTo("auth.email_taken")
        assertThat(serializer<InvalidCredentials>().descriptor.serialName)
            .isEqualTo("auth.invalid_credentials")
        assertThat(serializer<SessionExpired>().descriptor.serialName)
            .isEqualTo("auth.session_expired")
    }
}
