package org.example.project.shared.auth

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.jsonPrimitive
import org.example.project.shared.common.ApiError
import org.example.project.shared.common.encodeApiError

/**
 * Declared-contract freeze (ADR-0011): golden-dumps each auth operation → its Declared errors'
 * `@SerialName`s *and* HTTP statuses, both read off the endpoint's sealed lens. Adding a variant or
 * changing a status fails this test until the golden (and the sample list) is edited deliberately —
 * the codes and statuses are the wire contract for un-updatable clients.
 */
class AuthDeclaredErrorFreezeTest {
    // Hand-instantiated samples, one per variant; coverage against each lens's descriptor below.
    private val signupSamples: List<AuthSignupError> = listOf(EmailTaken)
    private val loginSamples: List<AuthLoginError> = listOf(InvalidCredentials)
    private val refreshSamples: List<AuthRefreshError> = listOf(SessionExpired)

    @Test
    fun `auth declared error contracts are frozen`() {
        assertThat(declaredContract(AuthApi.signup.error, signupSamples))
            .isEqualTo(mapOf("auth.email_taken" to 409))
        assertThat(declaredContract(AuthApi.login.error, loginSamples))
            .isEqualTo(mapOf("auth.invalid_credentials" to 401))
        assertThat(declaredContract(AuthApi.refresh.error, refreshSamples))
            .isEqualTo(mapOf("auth.session_expired" to 401))
        assertThat(AuthApi.logout.error.declaredCodes()).isEqualTo(emptySet<String>())
        assertThat(AuthApi.me.error.declaredCodes()).isEqualTo(emptySet<String>())
    }

    @Test
    fun `declared statuses are client-visible 4xx`() {
        // The client parses an ErrorEnvelope only out of a 4xx; a Declared error outside that
        // range would silently sever its own typed channel.
        (declaredContract(AuthApi.signup.error, signupSamples) +
                declaredContract(AuthApi.login.error, loginSamples) +
                declaredContract(AuthApi.refresh.error, refreshSamples))
            .forEach { (code, status) ->
                assertThat(status in 400..499, name = code).isEqualTo(true)
            }
    }
}

/**
 * `serialName → status` for a Declared lens from hand-instantiated [samples] (several variants
 * could carry required fields, so instances aren't derivable). The sample list must cover every
 * lens variant — asserted against the sealed descriptor, so a new variant fails here until a sample
 * (and thus a golden entry) is added.
 */
private fun <E : ApiError> declaredContract(
    lens: KSerializer<E>?,
    samples: List<E>,
): Map<String, Int> {
    requireNotNull(lens)
    val sampled = samples.associate { sample ->
        val code = encodeApiError(lens, sample).getValue("type").jsonPrimitive.content
        code to sample.status.value
    }
    require(sampled.keys == lens.declaredCodes()) {
        "Samples ${sampled.keys} must cover the lens's variants ${lens.declaredCodes()}"
    }
    return sampled
}

/** Every variant `serialName` of a Declared lens, from its sealed descriptor. */
private fun KSerializer<out ApiError>?.declaredCodes(): Set<String> {
    if (this == null) return emptySet()
    val subclasses = descriptor.getElementDescriptor(1)
    return (0 until subclasses.elementsCount)
        .map { subclasses.getElementDescriptor(it).serialName }
        .toSet()
}
