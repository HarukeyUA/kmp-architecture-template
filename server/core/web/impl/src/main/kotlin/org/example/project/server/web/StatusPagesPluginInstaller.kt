package org.example.project.server.web

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.statuspages.StatusPages
import kotlinx.coroutines.CancellationException
import org.example.project.shared.common.BadRequest as ApiBadRequest
import org.example.project.shared.common.Internal
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
            status(HttpStatusCode.Unauthorized) { call.respondError(Unauthorized) }
            exception<CancellationException> { _, cause -> throw cause }
            exception<BadRequestException> { call, _ ->
                call.respondError(ApiBadRequest(MALFORMED_BODY))
            }
            exception<ContentTransformationException> { call, _ ->
                call.respondError(ApiBadRequest(MALFORMED_BODY))
            }
            exception<JsonConvertException> { call, _ ->
                call.respondError(ApiBadRequest(MALFORMED_BODY))
            }
            exception<Throwable> { call, cause ->
                call.application.log.error("Unhandled exception", cause)
                call.respondError(Internal)
            }
        }
    }

    private companion object {
        const val MALFORMED_BODY = "malformed_body"
    }
}
