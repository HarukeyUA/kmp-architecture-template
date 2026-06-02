package org.example.project.core.network

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.context.raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.example.project.core.error.NetworkError
import org.example.project.shared.common.Endpoint
import org.example.project.shared.common.ErrorEnvelope

/**
 * Executes a Ktor HTTP request safely, catching exceptions and mapping failures to [NetworkError].
 *
 * @param block produces the HTTP response
 * @param transform extracts the desired value from a successful response
 */
suspend inline fun <T> executeSafe(
    crossinline block: suspend () -> HttpResponse,
    crossinline transform: suspend (HttpResponse) -> T,
): Either<NetworkError, T> = either {
    val response = catch({ block() }) { e -> raise(NetworkError.Connection(e)) }
    ensure(response.status.isSuccess()) { response.toNetworkError() }
    catch({ transform(response) }) { e -> raise(NetworkError.Serialization(e)) }
}

/**
 * Maps a non-2xx response to a typed error: a parseable 4xx [ErrorEnvelope] becomes
 * [NetworkError.Api] carrying the server's typed [org.example.project.shared.common.ApiError];
 * anything else (5xx, non-JSON, or a parse failure) falls back to [NetworkError.Http]. The
 * HttpClient's `ContentNegotiation` Json — built from the same seam multibinding as the server —
 * deserializes the polymorphic error, so an unknown code degrades to `UnknownApiError`.
 */
suspend fun HttpResponse.toNetworkError(): NetworkError {
    val apiError =
        if (status.value in CLIENT_ERROR_RANGE) {
            catch({ body<ErrorEnvelope>().error }) { e ->
                return NetworkError.Serialization(e)
            }
        } else {
            null
        }
    return apiError?.let { NetworkError.Api(it) } ?: NetworkError.Http(status.value)
}

private val CLIENT_ERROR_RANGE = 400..499

/** Executes a request and deserializes the response body to [T]. */
suspend inline fun <reified T> safeRequest(
    crossinline block: suspend () -> HttpResponse
): Either<NetworkError, T> = executeSafe(block) { it.body<T>() }

/**
 * Calls a typed [Endpoint] with a request [body]: builds the URL from [resource], dispatches the
 * endpoint's method, sends a JSON body only when the endpoint declares one, and decodes the
 * response to [Res] (or [Unit] when it declares none). The body and return types are checked
 * against the same [Endpoint] the server binds, so the two ends can't drift; failures map to
 * [NetworkError] exactly as [executeSafe] does.
 */
suspend inline fun <reified R : Any, reified Req : Any, reified Res : Any> HttpClient.call(
    endpoint: Endpoint<R, Req, Res>,
    resource: R,
    body: Req,
): Either<NetworkError, Res> =
    executeSafe({
        request(resource) {
            method = endpoint.method
            if (endpoint.request != null) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }) { response ->
        if (endpoint.response != null) response.body<Res>() else Unit as Res
    }

/** [call] for an endpoint that takes no request body. */
suspend inline fun <reified R : Any, reified Res : Any> HttpClient.call(
    endpoint: Endpoint<R, Unit, Res>,
    resource: R,
): Either<NetworkError, Res> = call(endpoint, resource, Unit)
