package org.example.project.core.network

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.context.raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import org.example.project.core.error.NetworkError
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
            catch({ body<ErrorEnvelope>().error }) { e -> return NetworkError.Serialization(e) }
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
