package org.example.project.shared.common

import io.ktor.http.HttpMethod
import kotlinx.serialization.KSerializer

/**
 * The typed contract for one HTTP **operation** — what a bare `@Resource` can't express: the
 * [method] it answers, the request body [Req] it accepts, and the response [Res] it returns. The
 * phantom [R]/[Req]/[Res] parameters let both ends compile-check the body and the return against
 * this single declaration; the nullable [request]/[response] serializers carry the same contract at
 * runtime (`null` ⇒ no body, i.e. [Unit]) and are the hook a future OpenAPI/golden dump would walk.
 *
 * The unit that owns a body and a return is the *operation*, not the resource: a single `@Resource`
 * can back several endpoints (e.g. `GET` and `POST` on the same collection), so each is keyed by
 * [method] too. Declared once per operation beside the `@Resource` it targets (see the `*Api`
 * objects); the client calls it via `HttpClient.call` and the server binds it via `Route.serve`, so
 * neither side can drift on method, body, or response.
 */
class Endpoint<R : Any, Req : Any, Res : Any>(
    val method: HttpMethod,
    val request: KSerializer<Req>?,
    val response: KSerializer<Res>?,
)
