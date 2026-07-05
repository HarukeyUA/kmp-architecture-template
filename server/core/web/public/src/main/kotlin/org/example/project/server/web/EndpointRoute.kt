package org.example.project.server.web

import arrow.core.Either
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.handle
import io.ktor.server.resources.resource
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.method
import org.example.project.shared.common.ApiError
import org.example.project.shared.common.Endpoint

/**
 * Binds a handler to a typed [Endpoint] that takes a request body: it registers the endpoint's
 * [Endpoint.method] on its [R] resource, receives the [Req] body, and folds the handler's
 * `Either<Failure<Err>, Res>` to HTTP — sending [successStatus] with the [Res] body, or just the
 * status when the endpoint declares no response.
 *
 * The failure channel is [Failure] tied to the endpoint's declared [Err] (ADR-0011): a service can
 * only put a Declared error here if this very [Endpoint] declares it, and Ambient errors ride along
 * regardless. Both arms unwrap to a plain [ApiError] before the existing [respondEither] path, so
 * status mapping is unchanged. Because the body and return types are checked against the very
 * [Endpoint] the client calls, the two ends can't drift. A body-less endpoint (`Req` = [Unit])
 * selects the [serve] overload with a one-argument handler, so "has a request body" is encoded in
 * the type rather than a runtime cast. The route stays dumb (resolve inputs, call the service, fold
 * the result); the success status is the one HTTP detail it still chooses, since the client only
 * branches on success vs. error.
 */
inline fun <reified R : Any, reified Req : Any, reified Res : Any, Err : ApiError> Route.serve(
    endpoint: Endpoint<R, Req, Res, Err>,
    successStatus: HttpStatusCode,
    crossinline handler: suspend RoutingContext.(R, Req) -> Either<Failure<Err>, Res>,
) {
    serveEndpoint(endpoint, successStatus) { resource -> handler(resource, call.receive<Req>()) }
}

/** [serve] for an endpoint that takes no request body (`Req` = [Unit]). */
inline fun <reified R : Any, reified Res : Any, Err : ApiError> Route.serve(
    endpoint: Endpoint<R, Unit, Res, Err>,
    successStatus: HttpStatusCode,
    crossinline handler: suspend RoutingContext.(R) -> Either<Failure<Err>, Res>,
) {
    serveEndpoint(endpoint, successStatus) { resource -> handler(resource) }
}

/**
 * Shared registration + fold for both [serve] overloads: registers [endpoint]'s [Endpoint.method]
 * on its [R] resource, unwraps the [produce]d `Either<Failure<Err>, Res>` to `Either<ApiError,
 * Res>` (both [Failure] arms carry a plain [ApiError]), and folds it to HTTP via [respondEither] —
 * [successStatus] with the [Res] body, or just the status when the endpoint declares no response.
 */
@PublishedApi
internal inline fun <reified R : Any, reified Res : Any, Err : ApiError> Route.serveEndpoint(
    endpoint: Endpoint<R, *, Res, Err>,
    successStatus: HttpStatusCode,
    crossinline produce: suspend RoutingContext.(R) -> Either<Failure<Err>, Res>,
) {
    resource<R> {
        method(endpoint.method) {
            handle<R> { resource ->
                call.respondEither(produce(resource).mapLeft { it.error }) { value ->
                    if (endpoint.response != null) respond(successStatus, value)
                    else respond(successStatus)
                }
            }
        }
    }
}
