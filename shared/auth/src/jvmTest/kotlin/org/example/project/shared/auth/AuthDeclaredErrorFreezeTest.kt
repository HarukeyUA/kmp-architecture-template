package org.example.project.shared.auth

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import org.example.project.shared.common.ApiError

/**
 * Declared-set freeze (ADR-0011): golden-dumps each auth operation → the `@SerialName`s of its
 * Declared errors, reached by walking the endpoint's error lens via `KClass.sealedSubclasses`. This
 * is the machine-checked answer to "what can this call return" — adding, removing, or re-pointing a
 * lens variant changes the dump and fails here, so the set can't drift silently. Lives in jvmTest
 * because `sealedSubclasses` is JVM-only reflection (kotlin-reflect); the golden-name freeze that
 * pins the codes themselves stays cross-platform in `AuthErrorGoldenTest`.
 */
@OptIn(InternalSerializationApi::class)
class AuthDeclaredErrorFreezeTest {
    @Test
    fun `auth declared error sets are frozen`() {
        val declared =
            mapOf(
                "signup" to AuthApi.signup.error.declaredDiscriminators(),
                "login" to AuthApi.login.error.declaredDiscriminators(),
                "refresh" to AuthApi.refresh.error.declaredDiscriminators(),
                "logout" to AuthApi.logout.error.declaredDiscriminators(),
                "me" to AuthApi.me.error.declaredDiscriminators(),
            )

        assertThat(declared)
            .isEqualTo(
                mapOf(
                    "signup" to setOf("auth.email_taken"),
                    "login" to setOf("auth.invalid_credentials"),
                    "refresh" to setOf("auth.session_expired"),
                    "logout" to emptySet(),
                    "me" to emptySet(),
                )
            )
    }
}

@OptIn(InternalSerializationApi::class)
private fun KClass<out ApiError>?.declaredDiscriminators(): Set<String> =
    this?.sealedSubclasses.orEmpty().map { it.serializer().descriptor.serialName }.toSet()
