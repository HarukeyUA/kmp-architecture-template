package org.example.project.shared.auth

import arrow.core.getOrElse
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import org.example.project.shared.common.FieldError

class AuthValidationTest {
    @Test
    fun `email is trimmed, lowercased, and accepted`() {
        assertThat(Email.of("  Alice@Example.COM ").getOrNull()?.value)
            .isEqualTo("alice@example.com")
    }

    @Test
    fun `blank email is rejected with a field code`() {
        assertThat(Email.of("   ").leftOrNull()).isEqualTo(FieldError("email", "blank"))
    }

    @Test
    fun `malformed email is rejected`() {
        assertThat(Email.of("not-an-email").leftOrNull()).isEqualTo(FieldError("email", "format"))
        assertThat(Email.of("not-an-email").getOrNull()).isNull()
    }

    @Test
    fun `short password is rejected`() {
        assertThat(Password.of("short").leftOrNull()).isEqualTo(FieldError("password", "too_short"))
    }

    @Test
    fun `valid password is accepted`() {
        val value = Password.of("hunter2hunter2").getOrElse { error("expected valid") }.value
        assertThat(value).isEqualTo("hunter2hunter2")
    }
}
