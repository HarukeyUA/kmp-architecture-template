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
    ensure(response.status.isSuccess()) { NetworkError.Http(response.status.value) }
    catch({ transform(response) }) { e -> raise(NetworkError.Serialization(e)) }
}

/** Executes a request and deserializes the response body to [T]. */
suspend inline fun <reified T> safeRequest(
    crossinline block: suspend () -> HttpResponse
): Either<NetworkError, T> = executeSafe(block) { it.body<T>() }
