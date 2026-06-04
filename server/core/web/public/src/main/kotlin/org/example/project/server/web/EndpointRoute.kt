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
 * `Either<ApiError, Res>` to HTTP — sending [successStatus] with the [Res] body, or just the status
 * when the endpoint declares no response.
 *
 * Because the body and return types are checked against the very [Endpoint] the client calls, the
 * two ends can't drift. A body-less endpoint (`Req` = [Unit]) selects the [serve] overload with a
 * one-argument handler, so "has a request body" is encoded in the type rather than a runtime cast.
 * The route stays dumb (resolve inputs, call the service, fold the result); the success status is
 * the one HTTP detail it still chooses, since the client only branches on success vs. error.
 */
inline fun <reified R : Any, reified Req : Any, reified Res : Any> Route.serve(
    endpoint: Endpoint<R, Req, Res>,
    successStatus: HttpStatusCode,
    crossinline handler: suspend RoutingContext.(R, Req) -> Either<ApiError, Res>,
) {
    serveEndpoint(endpoint, successStatus) { resource -> handler(resource, call.receive<Req>()) }
}

/** [serve] for an endpoint that takes no request body (`Req` = [Unit]). */
inline fun <reified R : Any, reified Res : Any> Route.serve(
    endpoint: Endpoint<R, Unit, Res>,
    successStatus: HttpStatusCode,
    crossinline handler: suspend RoutingContext.(R) -> Either<ApiError, Res>,
) {
    serveEndpoint(endpoint, successStatus) { resource -> handler(resource) }
}

/**
 * Shared registration + fold for both [serve] overloads: registers [endpoint]'s [Endpoint.method]
 * on its [R] resource and folds the [produce]d `Either<ApiError, Res>` to HTTP via [respondEither]
 * — [successStatus] with the [Res] body, or just the status when the endpoint declares no response.
 */
@PublishedApi
internal inline fun <reified R : Any, reified Res : Any> Route.serveEndpoint(
    endpoint: Endpoint<R, *, Res>,
    successStatus: HttpStatusCode,
    crossinline produce: suspend RoutingContext.(R) -> Either<ApiError, Res>,
) {
    resource<R> {
        method(endpoint.method) {
            handle<R> { resource ->
                call.respondEither(produce(resource)) { value ->
                    if (endpoint.response != null) respond(successStatus, value)
                    else respond(successStatus)
                }
            }
        }
    }
}
