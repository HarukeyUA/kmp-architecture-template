package org.example.project.server.web

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.PayloadTooLargeException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.contentType
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import kotlinx.coroutines.CancellationException
import org.example.project.shared.common.BadRequest as ApiBadRequest
import org.example.project.shared.common.Internal
import org.example.project.shared.common.PayloadTooLarge
import org.example.project.shared.common.RateLimited
import org.example.project.shared.common.Unauthorized

/**
 * Safety net for **unexpected** exceptions only: log the cause and return a generic [Internal]
 * `ApiError` (`500`), never leaking internals. Expected failures travel as typed `Either<ApiError,
 * T>` and are mapped by the route layer via `respondEither` (ADR-0005).
 */
@Inject
@ContributesIntoSet(AppScope::class)
class StatusPagesPluginInstaller(private val statusMappers: Set<ApiErrorStatusMapper>) :
    PluginInstaller {
    override val order: PluginOrder = PluginOrder.STATUS_PAGES

    override fun Application.install() {
        installApiErrorStatusMappers(statusMappers)
        install(StatusPages) {
            // The bearer challenge emits a body-less 401 (an OutgoingContent.NoContent); normalise
            // only that into the standard envelope. A 401 the app itself produced via respondError
            // already carries its ErrorEnvelope, so leave it untouched instead of rebuilding (and
            // potentially clobbering) it.
            status(HttpStatusCode.Unauthorized) {
                if (content is OutgoingContent.NoContent) call.respondError(Unauthorized)
            }
            // The RateLimit plugin rejects with a body-less 429 after stamping Retry-After (its
            // default modifyResponse); normalise that into the typed envelope, lifting the header
            // into the actionable field. Same leave-enveloped-responses-alone rule as the 401.
            status(HttpStatusCode.TooManyRequests) {
                if (content is OutgoingContent.NoContent) {
                    val retryAfterSeconds =
                        call.response.headers[HttpHeaders.RetryAfter]?.toLongOrNull()
                    call.respondError(RateLimited(retryAfterSeconds))
                }
            }
            exception<CancellationException> { _, cause -> throw cause }
            // Must be handled explicitly: PayloadTooLargeException *is* a
            // ContentTransformationException, and the generic handler below would mislabel an
            // over-limit body as a 400 "malformed_body". StatusPages dispatches on the most
            // specific registered type, so this wins for over-limit bodies.
            exception<PayloadTooLargeException> { call, cause ->
                call.application.log.debug(
                    "Rejected over-limit request body on {} {}: {}",
                    call.request.httpMethod.value,
                    call.request.path(),
                    cause.message,
                )
                call.respondError(PayloadTooLarge)
            }
            exception<BadRequestException> { call, cause -> call.rejectMalformedBody(cause) }
            exception<ContentTransformationException> { call, cause ->
                call.rejectMalformedBody(cause)
            }
            exception<JsonConvertException> { call, cause -> call.rejectMalformedBody(cause) }
            exception<Throwable> { call, cause ->
                call.application.log.error("Unhandled exception", cause)
                call.respondError(Internal)
            }
        }
    }

    /**
     * A malformed request body is a client fault (`400`), so the client gets only the opaque
     * [MALFORMED_BODY] reason. The cause is logged at DEBUG for diagnosis — its *type* and the
     * request route only, never its message, which can echo the offending body (e.g. a password on
     * `POST /auth/login`). It stays at DEBUG, not ERROR: a client sending garbage is not a server
     * fault and must not pollute error telemetry.
     */
    private suspend fun ApplicationCall.rejectMalformedBody(cause: Throwable) {
        application.log.debug(
            "Rejected malformed request body on {} {} ({}): {}",
            request.httpMethod.value,
            request.path(),
            request.contentType(),
            cause::class.simpleName,
        )
        respondError(ApiBadRequest(MALFORMED_BODY))
    }

    private companion object {
        const val MALFORMED_BODY = "malformed_body"
    }
}
