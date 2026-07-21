package org.example.project.server

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import org.example.project.shared.auth.AuthApi
import org.example.project.shared.common.ApiError
import org.example.project.shared.common.ErrorEnvelope
import org.example.project.shared.common.decodeAmbientApiError
import org.example.project.shared.common.decodeDeclaredApiError
import org.example.project.shared.notes.NotesApi

/**
 * Decodes this error response's envelope through every Declared lens the integration suites
 * exercise, falling back to the Ambient (common/unknown) path — the test-side equivalent of what
 * the client does with the one lens of the endpoint it called. Lets an assertion compare against a
 * typed error without naming the lens at every call site.
 */
suspend fun HttpResponse.decodedError(): ApiError {
    val envelope = body<ErrorEnvelope>()
    return sequenceOf(
            AuthApi.signup.error,
            AuthApi.login.error,
            AuthApi.refresh.error,
            NotesApi.create.error,
        )
        .filterNotNull()
        .firstNotNullOfOrNull { lens -> decodeDeclaredApiError(lens, envelope.error) }
        ?: decodeAmbientApiError(envelope.error, status)
}
