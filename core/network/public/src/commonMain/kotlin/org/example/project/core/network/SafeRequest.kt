package org.example.project.core.network

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
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
    if (response.status.isSuccess()) {
        catch({ transform(response) }) { e -> raise(NetworkError.Serialization(e)) }
    } else {
        raise(NetworkError.Http(response.status.value))
    }
}

/** Executes a request and deserializes the response body to [T]. */
suspend inline fun <reified T> safeRequest(
    crossinline block: suspend () -> HttpResponse
): Either<NetworkError, T> = executeSafe(block) { it.body<T>() }
