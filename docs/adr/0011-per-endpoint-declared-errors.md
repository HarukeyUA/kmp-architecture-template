# 0011. Per-endpoint Declared errors — `Endpoint` grows an `Err` parameter

- Status: Accepted. Resolves the pin deferred in ADR-0005; supersedes its "per-domain sealed lens" candidate.
- Date: 2026-07-05

## Context

ADR-0005 settled the wire error model (open polymorphic `ApiError` in an `ErrorEnvelope`, forward-compat `UnknownApiError` fallback, per-domain error types distributed across `:shared:<domain>` modules) and deferred one thing: how the client gets exhaustive, compile-checked handling of the errors a call can produce. The deferred candidate was a per-domain sealed lens (`is NotesApiError -> …`). We want stronger: the client should know *exactly* which errors a given operation can return — per endpoint, not per domain — and the guarantee must be enforced where errors are minted, not hoped for at the point of consumption.

## Decision

**Granularity: per operation.** `Endpoint<R, Req, Res>` becomes `Endpoint<R, Req, Res, Err : ApiError>` and gains `val error: KClass<out Err>?` alongside the nullable body serializers — `null` means "no Declared errors" (then `Err = Nothing` and the client's Declared arm is statically unreachable), mirroring `null` = no body. This supersedes the per-domain lens: a domain lens only says "some notes error," while the endpoint set is the machine-checked answer to "what can *this call* return."

**Declared vs Ambient (see CONTEXT.md).** An operation's `Err` set contains only types minted in the owning domain's Contract module. Cross-cutting errors in `:shared:common` (`Unauthorized`, `Validation`, `NotFound`, `RateLimited`, `Internal`, `UnknownApiError`, …) are always Ambient — they can never implement a domain's sealed interface anyway (dependency direction, ADR-0001). The rule: **if the client must react specifically, the domain mints a Declared error, even when a similar common shape exists** (e.g. `auth.invalid_credentials` rather than common `Unauthorized`). Common errors are protocol plumbing, handled once, centrally.

**Lens shape.** One sealed interface per operation that has Declared errors (`sealed interface NotesCreateError : ApiError`), named `<Domain><Operation>Error`, minted lazily — no empty placeholder per endpoint. An error shared by several operations implements several lenses. Serialization is untouched: variants still register in the domain `SerializersModule` against polymorphic `ApiError`; the golden-name tests keep freezing discriminators, and a new **declared-set freeze test** per domain golden-dumps `operation → declared discriminators` so the set cannot change silently.

**Client consumption.** The wrapper is client-owned (`:client:core:error`), not shared — the Seam holds Wire types and the `Endpoint` contract only; the two sides' failure shapes genuinely differ (the server never produces `UnknownApiError` or transport failures):

```kotlin
sealed interface CallFailure<out E : ApiError> : AppError {
    data class Declared<E : ApiError>(val error: E) : CallFailure<E>   // exhaustive when
    data class Ambient(val error: ApiError) : CallFailure<Nothing>     // central renderer path
    data class Transport(val error: NetworkError) : CallFailure<Nothing> // offline / 5xx / decode
}
```

`NetworkError.Api` is deleted (subsumed). Narrowing is an `isInstance` check against the endpoint's `KClass` — no per-endpoint mapping code. A known-but-undeclared error (contract drift, version skew) fails the check and lands in Ambient: the same safety valve `UnknownApiError` gives unknown discriminators, extended to undeclared ones.

**Server enforcement — compile-time, not courtesy.** Handlers and services return `Either<Failure<Err>, T>` with a server-side two-arm wrapper in `:server:core:web` and Raise helpers (`declared(e)` / `ambient(e)`), so a service can only put an error in the Declared channel if the endpoint declares it. `Route.serve(endpoint)` requires the matching `Failure<Err>`, unwraps to `ApiError`, and the existing `ErrorResponder`/status-mapping path is unchanged. Without this, the client's exhaustive `when` is built on sand and drift is silently absorbed by Ambient.

**Declared errors cross the client unmapped — a deliberate carve-out from ADR-0003.** Data DTOs are mapped because Wire and Domain model evolve at different rates; Declared errors are terminal values whose whole purpose is to be branched on once, and a mapping layer would add a drift surface while deleting the compile-time guarantee. Default posture: repositories preserve the parameter (`Either<CallFailure<NotesCreateError>, Note>`). This is permission, not enforcement — a repository that recovers errors internally or narrows its contract mints its own error hierarchy and maps.

**Disclosure boundary survives (ADR-0005).** Minting per-domain errors does not license finer disclosure: `auth.invalid_credentials` is still one collapsed variant (user-not-found vs wrong-password stays indistinguishable; the dummy-verify timing defense is unaffected), as is `auth.session_expired`. A consequence: `Unauthorized` now means exactly one thing everywhere — Access token missing/expired/invalid — so a central "force re-auth on `Unauthorized`" handler becomes safe to write. Corollary: an operation not authenticated by Access token (e.g. `refresh`, which presents a Session) must declare its credential failure, never reuse `Unauthorized`.

## Initial declared sets

`signup → EmailTaken`; `login → InvalidCredentials`; `refresh → SessionExpired`; `notes.create → NotesQuotaExceeded`; `logout`, `me`, `notes.list`, `notes.delete` → none (logout is idempotent; delete keeps ambient `NotFound` — making delete idempotent-success is a separate wire-semantics decision, out of scope here). Five of eight operations declare nothing: ceremony exists only where a real client decision exists.

## Considered options

- **Per-domain sealed lens (ADR-0005's deferred candidate)** — cheap and additive, but exhaustiveness is over the domain's whole vocabulary, not the actual endpoint's; degrades as domains grow. Rejected in favor of per-endpoint sets.
- **Endpoints claiming common variants into `Err`** via wrapping variants + per-endpoint mapping functions — full expressiveness, but N-times-repeated mapping ceremony that drifts and discourages declaring errors at all. Rejected for "common is always Ambient; mint a domain error when the client cares."
- **No server enforcement / runtime WARN only** — zero churn, but the declared set becomes aspirational; drift is silent (or visible only on executed paths in logs). Rejected.
- **Coarse shared lenses per domain-subset** (`NotesWriteError` for create/update/delete) — recreates the lens problem at smaller scale; `delete` would be forced to handle `QuotaExceeded`. Rejected for strict per-operation lenses with multi-membership.

## Consequences

- Forward-compat holds end-to-end: an old client routes a new discriminator to `UnknownApiError` → Ambient; a recompiled client gets the compile error in its `when` — which is the feature, not a bug.
- Wire changes on this branch: login `common.unauthorized` → `auth.invalid_credentials`; refresh mints `auth.session_expired`; golden tests updated. Judged safe pre-1.0.
- The `Failure<E>` wrapper reaches service signatures on the server — accepted, since services are where errors originate and routes stay dumb registrars.
- Client features may enrich a Declared error with client-only state by wrapping ad hoc at the call site; there is deliberately no standing translation layer to hang it on.
