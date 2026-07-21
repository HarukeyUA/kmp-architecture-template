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
import org.example.project.core.error.CallFailure
import org.example.project.core.error.NetworkError
import org.example.project.shared.common.ApiError
import org.example.project.shared.common.Endpoint
import org.example.project.shared.common.ErrorEnvelope
import org.example.project.shared.common.decodeAmbientApiError
import org.example.project.shared.common.decodeDeclaredApiError

/**
 * Executes a Ktor HTTP request safely, catching exceptions and mapping failures to [NetworkError].
 * The non-[Endpoint] escape hatch: a raw request has no Declared-error contract, so every non-2xx
 * is a [NetworkError.Http]. Endpoint calls use [call], which narrows the wire error instead.
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

/**
 * Calls a typed [Endpoint] with a request [body]: builds the URL from [resource], dispatches the
 * endpoint's method, sends a JSON body only when the endpoint declares one, and decodes the
 * response to [Res] (or [Unit] when it declares none). The body and return types are checked
 * against the same [Endpoint] the server binds, so the two ends can't drift.
 *
 * Failures land in [CallFailure] (ADR-0011): a connection drop, a 5xx, or a decode/parse failure is
 * [CallFailure.Transport]; a parsed 4xx envelope is [CallFailure.Declared] when its [ApiError] is
 * an instance of the endpoint's declared [Endpoint.error] lens, and [CallFailure.Ambient] otherwise
 * — which folds in both cross-cutting errors and known-but-undeclared ones (the drift safety
 * valve).
 */
suspend inline fun <
    reified R : Any,
    reified Req : Any,
    reified Res : Any,
    Err : ApiError,
> HttpClient.call(
    endpoint: Endpoint<R, Req, Res, Err>,
    resource: R,
    body: Req,
): Either<CallFailure<Err>, Res> = either {
    val response =
        catch({
            request(resource) {
                method = endpoint.method
                if (endpoint.request != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            }
        }) { e ->
            raise(CallFailure.Transport(NetworkError.Connection(e)))
        }

    ensure(response.status.isSuccess()) { endpoint.toCallFailure(response) }

    catch({ if (endpoint.response != null) response.body<Res>() else Unit as Res }) { e ->
        raise(CallFailure.Transport(NetworkError.Serialization(e)))
    }
}

/** [call] for an endpoint that takes no request body. */
suspend inline fun <reified R : Any, reified Res : Any, Err : ApiError> HttpClient.call(
    endpoint: Endpoint<R, Unit, Res, Err>,
    resource: R,
): Either<CallFailure<Err>, Res> = call(endpoint, resource, Unit)

/**
 * Maps a non-2xx response to a [CallFailure]. A parseable 4xx [ErrorEnvelope] is narrowed against
 * the endpoint's declared [Endpoint.error] lens: an instance becomes [CallFailure.Declared],
 * anything else (cross-cutting, unknown, or a known-but-undeclared code from version skew) becomes
 * [CallFailure.Ambient]. A 5xx or an unparseable body is [CallFailure.Transport]. Decode success
 * through the lens *is* the narrowing to Declared; an unknown code degrades to `UnknownApiError`
 * rather than throwing.
 */
suspend fun <Err : ApiError> Endpoint<*, *, *, Err>.toCallFailure(
    response: HttpResponse
): CallFailure<Err> {
    if (response.status.value !in CLIENT_ERROR_RANGE) {
        return CallFailure.Transport(NetworkError.Http(response.status.value))
    }
    val raw =
        catch({ response.body<ErrorEnvelope>().error }) { e ->
            return CallFailure.Transport(NetworkError.Serialization(e))
        }
    val declared = error?.let { lens -> decodeDeclaredApiError(lens, raw) }
    return if (declared != null) CallFailure.Declared(declared)
    else CallFailure.Ambient(decodeAmbientApiError(raw, response.status))
}

private val CLIENT_ERROR_RANGE = 400..499
