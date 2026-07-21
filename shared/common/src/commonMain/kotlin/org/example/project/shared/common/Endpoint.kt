package org.example.project.shared.common

import io.ktor.http.HttpMethod
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PolymorphicKind

/**
 * The typed contract for one HTTP **operation** — what a bare `@Resource` can't express: the
 * [method] it answers, the request body [Req] it accepts, the response [Res] it returns, and the
 * Declared errors [Err] it commits to. The phantom [R]/[Req]/[Res]/[Err] parameters let both ends
 * compile-check the body, the return, and the failure channel against this single declaration; the
 * nullable [request]/[response] serializers carry the same contract at runtime (`null` ⇒ no body,
 * i.e. [Unit]) and are the hook a future OpenAPI/golden dump would walk.
 *
 * [error] mirrors the body serializers for the failure channel: it is the sealed serializer of this
 * operation's Declared-error lens (`<Domain><Operation>Error`, a `@Serializable sealed interface`
 * whose variants are the errors this call may return), and `null` ⇒ **no Declared errors** —
 * exactly as `null` on a body serializer ⇒ no body. When there are none, [Err] is [Nothing], so the
 * client's Declared arm is statically unreachable and only Ambient (cross-cutting,
 * `:shared:common`) errors remain. The client narrows by decoding through the lens; a
 * known-but-undeclared code (contract drift, version skew) fails that decode and falls to the
 * Ambient path — see [decodeDeclaredApiError]. Declared errors are always minted in the owning
 * domain's Contract module — cross-cutting errors can never be Declared (dependency direction) and
 * ride every operation Ambiently (see ADR-0011).
 *
 * The unit that owns a body and a return is the *operation*, not the resource: a single `@Resource`
 * can back several endpoints (e.g. `GET` and `POST` on the same collection), so each is keyed by
 * [method] too. Declared once per operation beside the `@Resource` it targets (see the `*Api`
 * objects); the client calls it via `HttpClient.call` and the server binds it via `Route.serve`, so
 * neither side can drift on method, body, response, or declared errors.
 */
class Endpoint<R : Any, Req : Any, Res : Any, Err : ApiError>(
    val method: HttpMethod,
    val request: KSerializer<Req>?,
    val response: KSerializer<Res>?,
    val error: KSerializer<Err>?,
) {
    init {
        // A lens missing @Serializable silently resolves to an OPEN polymorphic serializer, which
        // would make every Declared error ride Ambient. Failing here surfaces it at `*Api`
        // class-load, i.e. in any test that touches the endpoint.
        require(error == null || error.descriptor.kind == PolymorphicKind.SEALED) {
            "Endpoint error lens must be a @Serializable sealed interface " +
                "(got kind ${error?.descriptor?.kind} for ${error?.descriptor?.serialName})"
        }
    }
}
