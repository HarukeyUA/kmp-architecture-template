# 0005. Error model — polymorphic `ApiError` with forward-compat fallback

- Status: Accepted — the wire model below stands; the deferred per-domain sealed grouping is **resolved by ADR-0011** (per-endpoint Declared errors), which supersedes the lens candidate and the interim stop-gap.
- Date: 2026-05-30 (amended 2026-07-05)

## Context

We need typed errors that cross the Seam, survive forward-compat (old, un-updatable clients), integrate the client's existing `AppError`/`ErrorRenderer` pipeline, and never let the Seam depend on the client (umbrella law, ADR-0001).

## Decision (settled)

- **Wire error** = `ApiError` carried in `ErrorEnvelope { error, requestId }`. `ApiError` is an **open polymorphic** base; kotlinx `polymorphicDefaultDeserializer` routes any unrecognized discriminator to `UnknownApiError(code, raw)` — so a newer server's error **degrades** on an old client instead of crashing it.
- **Frozen, namespaced `@SerialName`** per variant (`common.unauthorized`, `notes.quota_exceeded`), guarded by a **golden-set freeze test** (sibling to the migration drift test, ADR-0007).
- **Registration is assembled outside `:shared`** — the dependency direction forbids `:shared:common` depending on its domains. Each domain exposes a `SerializersModule`; each side's `:impl` contributes it via Metro `@ContributesIntoSet`; `:client:core:network` and `:server:core:web` build one `Json` from the multibound set. kotlinx throws on a duplicate `@SerialName` at module-build time, giving global uniqueness for free.
- **Services return `Either<ApiError, T>`** *(amended by ADR-0011: now `Either<Failure<Err>, T>` — same principle, with the Declared channel compile-checked)*. `ApiError` is *semantic*, not HTTP — the route maps it to an `HttpStatusCode` in one `when` in `:server:core:web`. `Either` for expected failures; **StatusPages only for unexpected exceptions** → log + generic `Internal`, never leak.
- `ApiError` is the **information-disclosure boundary**: no `UserNotFound`-vs-`WrongPassword`; both collapse to a single variant *(originally `Unauthorized`; since ADR-0011, the Declared `auth.invalid_credentials` — still one collapsed error)*.
- **Client integration** *(amended by ADR-0011)*: `ApiError` stays pure in `:shared`; it is wrapped client-side in a carrier that implements `AppError` and rides the existing `DelegatingError`/`ErrorRenderer` pipeline to a localized string. ~~The carrier is `NetworkError.Api(apiError)`, filled by `executeSafe` from a 4xx envelope.~~ ADR-0011 replaced the carrier with `CallFailure<E>` (`Declared`/`Ambient`/`Transport`) and deleted `NetworkError.Api`.

## Deferred / open — resolved by ADR-0011

*(Historical: the question below was resolved 2026-07-05 by ADR-0011, which chose per-**endpoint** Declared error sets over the per-domain lens sketched here.)*

Whether each `:shared:<domain>` adds a sealed `XApiError : ApiError` grouping to serve as a **client-side narrowing lens** — `when (e) { is NotesApiError -> /* exhaustive */ ; is Conflict -> … ; else -> generic }` — giving autocomplete and compile-forced exhaustive handling of a domain's errors, while common/unknown fall to `else`, with forward-compat preserved (old clients route unknown variants to `UnknownApiError` → `else`). To be revisited once the rest of the server shape exists and can be experimented against.

### Considered options for the deferred grouping

- **Loose `ApiError` everywhere (A)** — simplest, but the client gets no signal of which errors a given call can produce (no autocomplete, no compile-time nudge).
- **Per-domain sealed *return type* (B)** — documents the error set, but forces awkward composition of cross-cutting errors into each domain's sealed type (cross-cutting errors aren't part of a domain).
- **Sealed grouping as a client-side *lens*, loose service return (synthesis)** — `is XApiError` narrows for exhaustive domain handling while common/unknown fall to `else`; B's discoverability with A's simplicity, forward-compat intact. Leading candidate, deferred for hands-on evaluation.

## Interim stop-gap (superseded by ADR-0011)

Open polymorphic `ApiError` in `:shared:common` with cross-cutting variants (`Unauthorized`, `Forbidden`, `NotFound`, `Conflict`, `Validation`, `RateLimited`, `Internal`, + `UnknownApiError` fallback); per-domain subtypes declared **directly** as `: ApiError` (no sealed grouping yet); services return loose `Either<ApiError, T>`; the client matches `is X -> … ; else -> generic`. Adding the sealed grouping later is **purely additive** (change `: ApiError` to `: XApiError : ApiError`) and non-breaking — so the stop-gap forecloses nothing.

## Consequences

- Forward-compat holds: old clients route unknown error codes to `UnknownApiError` and never crash; exhaustiveness (if the grouping is later adopted) only bites a client recompiled against the new contract.
- No server code exists yet — this is a design to apply when the server is scaffolded.
- ~~Tracked as a pin until resolved.~~ Pin resolved by ADR-0011.
