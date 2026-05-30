package org.example.project.server.observability

/**
 * The per-request correlation id surfaced in structured logs (via MDC) and echoed back in the
 * response header. The same id rides in the `ErrorEnvelope.requestId` (ADR-0005), so a user-facing
 * error can be traced straight to its server log line.
 */
object Correlation {
    const val MDC_KEY: String = "requestId"
    const val HEADER: String = "X-Request-Id"
}
