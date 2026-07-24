package org.example.project.server.testing

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.example.project.shared.auth.AuthResource
import org.example.project.shared.auth.SignupRequest
import org.example.project.shared.auth.TokensResponse

/**
 * Signs up a fresh account through the real route and returns its token pair. The shared preamble
 * of every authenticated flow; a test asserting on the signup response itself posts inline instead.
 * [TestPostgres.resetSchema] runs per `serverTest`, so the default email never collides across
 * tests.
 */
suspend fun signupViaApi(
    http: HttpClient,
    email: String = "alice@example.com",
    password: String = "hunter2hunter2",
): TokensResponse {
    val signup =
        http.post(AuthResource.Signup()) {
            contentType(ContentType.Application.Json)
            setBody(SignupRequest(email, password))
        }
    check(signup.status == HttpStatusCode.Created) {
        "signup fixture expected 201, got ${signup.status}"
    }
    return signup.body()
}
