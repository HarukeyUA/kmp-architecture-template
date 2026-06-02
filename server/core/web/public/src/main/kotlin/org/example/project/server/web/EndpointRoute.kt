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
 * Binds a handler to a typed [Endpoint] under the current route: it registers the endpoint's
 * [Endpoint.method] on its [R] resource, receives the [Req] body only when the endpoint declares
 * one, and folds the handler's `Either<ApiError, Res>` to HTTP via [respondEither] — sending
 * [successStatus] with the [Res] body, or just the status when the endpoint declares no response.
 *
 * Because the body and return types are checked against the very [Endpoint] the client calls, the
 * two ends can't drift. The route stays dumb (resolve inputs, call the service, fold the result);
 * the success status is the one HTTP detail it still chooses, since the client only branches on
 * success vs. error.
 */
inline fun <reified R : Any, reified Req : Any, reified Res : Any> Route.serve(
    endpoint: Endpoint<R, Req, Res>,
    successStatus: HttpStatusCode,
    crossinline handler: suspend RoutingContext.(R, Req) -> Either<ApiError, Res>,
) {
    resource<R> {
        method(endpoint.method) {
            handle<R> { resource ->
                val body: Req = if (endpoint.request != null) call.receive<Req>() else Unit as Req
                call.respondEither(handler(resource, body)) { value ->
                    if (endpoint.response != null) respond(successStatus, value)
                    else respond(successStatus)
                }
            }
        }
    }
}
