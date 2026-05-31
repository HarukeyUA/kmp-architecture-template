# Fullstack Architecture — Server & Shared Seam

> **Companion to [`ARCHITECTURE.md`](ARCHITECTURE.md)**, which documents the client. This file covers the server, the shared **Seam**, and the fullstack module topology that joins them.
>
> **Status: in progress** (see [`docs/IMPLEMENTATION_PLAN.md`](docs/IMPLEMENTATION_PLAN.md) for the phased plan). **Landed:** Phase 1 — client modules under `:client:*`; Phase 2 — the `:server:app` skeleton (boots against Postgres, `/health` + `/metrics`, Flyway, Metro-on-JVM graph, migration drift test); Phase 3 — the `:shared:common` seam + the polymorphic `ApiError` error pipeline end-to-end (stop-gap: no sealed grouping yet); Phase 4 — the **auth tracer bullet** (the validation gate): credential/session seam, Argon2id, opaque sessions + Caffeine cache, secure client token storage (KSafe), the 401→Login interceptor, and the platform `ApiConfig` — proven by a server Testcontainers integration test and a client MockEngine test; Phase 5 — the **notes** domain (this section's running example, now real): cheap replication of the slice shape plus a **cross-domain** `NotesService → AuthService` call (resolving each note's author through auth's `:public`, never its tables) and the per-account `notes.quota_exceeded` budget — added with **zero lines** in `:server:app`, and the module-assert task rejects a deliberate reach into another domain's `:impl`. **Pending:** hardening + deploy (Phase 6) and beyond. Rationale and rejected alternatives live in [`docs/adr/`](docs/adr/README.md); vocabulary lives in [`CONTEXT.md`](CONTEXT.md).

The goal: a Compose Multiplatform client and a Ktor server in **one Gradle build**, joined by a shared contract so the two halves feel like "one whole thing" — while staying changeable and not foreclosing horizontal scale. The unity comes from the **type-safe Seam**, not from interleaving code.

---

## 1. Module topology — three umbrellas

One Gradle build, three top-level umbrellas. The umbrella name *is* the dependency rule. ([ADR-0001](docs/adr/0001-three-umbrella-structure.md))

```
:client:*    Compose Multiplatform app   (today's :core:* and :feature:* move under here)
:shared:*    the Seam (contracts)          per-domain modules + :shared:common
:server:*    Ktor server
```

### Dependency law

| Umbrella   | May depend on                                  |
|------------|------------------------------------------------|
| `:client:*`| `:shared:*`, other `:client:*`                 |
| `:server:*`| `:shared:*`, other `:server:*`                 |
| `:shared:*`| `:shared:*` only                               |
| —          | `:client:*` ↔ `:server:*` is **forbidden**     |

`:shared`'s external dependency surface is **rationed** to `kotlinx.serialization`, `ktor-resources`, `arrow-core`, `kotlinx.datetime` — and nothing else. No Compose/Decompose, no Ktor client/server engines, no Exposed, no DataStore. This single rule keeps the Seam from rotting into a god module.

### Enforcement

Extends the existing settings/build assertions (see `ARCHITECTURE.md` → Module Structure):

| Rule | Where |
|------|-------|
| Umbrella dependency law + `:shared` dep-surface | `assertModuleDependencies` (extended) |
| Umbrella + `public`/`impl` naming for server modules | `module-structure-assert` (extended) |

### Target layout

```
:client:core:*           ← was :core:*
:client:feature:*        ← was :feature:*
:client:composeApp, :client:androidApp, :client:desktopApp

:shared:common           cross-cutting wire types (ApiError base, value types, envelopes)
:shared:<domain>         per-domain contract (routes + DTOs + shape validation + errors) — flat, all-public

:server:core:database:public|impl
:server:core:web:public|impl
:server:core:auth:public|impl
:server:core:observability:public|impl
:server:feature:<domain>:public|impl
:server:feature:<domain>:testing      (shared fakes, optional)
:server:app              composition root: Metro graph + main() + Ktor install + Flyway + route mounting
```

---

## 2. The Seam (`:shared`)

The Seam holds the **Wire** and nothing else. ([ADR-0003](docs/adr/0003-share-the-wire-not-the-domain.md))

| Lives in the Seam | Does **not** live in the Seam |
|-------------------|-------------------------------|
| `@Resource` typed routes | Domain models (each side owns its own) |
| `@Serializable` DTOs (`*Request` / `*Response`) | Business logic |
| Pure **shape** validation (smart constructors) | Stateful validation |
| The `ApiError` taxonomy | Rendering / UI concerns / DB concerns |

- **`:shared:<domain>`** — one per Domain (`:shared:notes`, `:shared:account`, …). Flat module, all-public by nature, no `public`/`impl` split (it's pure contract).
- **`:shared:common`** — cross-cutting wire types: the `ApiError` base + cross-cutting error variants, the `ErrorEnvelope`, shared value types.

**Principle — share the wire, never the domain.** Each side maps Wire ↔ its own Domain model. On the client this is the existing `toModel()` extension-function convention (`ARCHITECTURE.md` → Model Mapping Pipeline); DTOs that live today in each client feature's `:public/data/models/` **relocate into `:shared:<domain>`**. On the server, the route layer maps Wire ↔ server domain.

---

## 3. The Contract — REST over Ktor Resources

([ADR-0002](docs/adr/0002-rest-ktor-resources-contract.md)) Routes are typed `@Resource` classes in the Seam, consumed by both `ktor-client-resources` and `ktor-server-resources` — a single source of truth for paths and params. All routes under a `/v1` prefix.

```kotlin
// :shared:notes
@Resource("/v1/notes")
class NotesResource {
    @Resource("{id}")
    class ById(val parent: NotesResource = NotesResource(), val id: String)
}
```

Wire evolution is **additive**: add nullable/defaulted fields freely, `ignoreUnknownKeys = true` on the client. A `/v2` prefix exists only if ever genuinely forced. Real-time (SSE/WS) is **deferred** and will be an enhancement layer over this REST baseline with graceful degradation — never a dependency.

---

## 4. Validation split

([ADR-0004](docs/adr/0004-validation-split.md)) Boundary rule: **"needs state or identity? → server-only; otherwise → shared."**

```kotlin
// :shared:notes — PURE shape validation, reused on BOTH sides
@JvmInline value class NoteText private constructor(val value: String) {
    companion object {
        fun of(raw: String): Either<ValidationError, NoteText> = either {
            ensure(raw.isNotBlank()) { ValidationError.field("text", "blank") }
            ensure(raw.length <= 10_000) { ValidationError.field("text", "too_long") }
            NoteText(raw)
        }
    }
}
```

- **Shared (Seam):** required/length/format/range/enum/size — smart constructors returning `Either<ValidationError, T>`.
- **Server-only:** uniqueness, existence, authorization, quota — needs the DB or `Principal`.
- The server **always re-validates shape.** The duplication is the security boundary; client validation is a UX optimization only.

---

## 5. Error model

([ADR-0005](docs/adr/0005-error-model.md) — *partially deferred*) The wire error is an **open polymorphic `ApiError`** in an `ErrorEnvelope`, with a forward-compat `UnknownApiError` fallback so a newer server's error degrades on an old client instead of crashing it.

```kotlin
// :shared:common
interface ApiError                                  // open polymorphic base
@Serializable @SerialName("common.unauthorized")  data object Unauthorized : ApiError
@Serializable @SerialName("common.not_found")      data class NotFound(val resource: String) : ApiError
@Serializable @SerialName("common.conflict")       data class Conflict(val reason: String? = null) : ApiError
@Serializable @SerialName("common.validation")     data class Validation(val fields: List<FieldError>) : ApiError
@Serializable @SerialName("common.rate_limited")   data class RateLimited(val retryAfterSeconds: Long? = null) : ApiError
@Serializable @SerialName("common.internal")       data object Internal : ApiError
data class UnknownApiError(val code: String, val raw: JsonObject?) : ApiError   // via default deserializer

@Serializable data class ErrorEnvelope(val error: ApiError, val requestId: String? = null)
```

**Registration is assembled outside `:shared`** (the dependency direction forbids `:shared:common` knowing its domains). Each domain exposes a `SerializersModule`; each side's `:impl` contributes it via Metro `@ContributesIntoSet`; `:client:core:network` and `:server:core:web` build one `Json` from the multibound set. A duplicate `@SerialName` fails at module-build time → global uniqueness for free. `@SerialName`s are namespaced (`notes.quota_exceeded`) and frozen by a **golden-set test**.

> **Implemented (Phase 3).** Both sides build their `Json` via the shared `buildSeamJson(domainModules)` (folds the multibound set onto `commonApiErrorSerializersModule`), so the wire format is byte-identical. `UnknownApiError` is produced by a `defaultDeserializer` on the polymorphic `ApiError`. One Kotlin/Native subtlety, pre-solved: a bare `serializer<ApiError>()` can't resolve an *interface* serializer at runtime on Native, so `ApiError` always crosses the wire **inside `ErrorEnvelope`** — whose generated serializer wires the polymorphic field at compile time on every target. (Serialize a bare `ApiError` only via `PolymorphicSerializer(ApiError::class)`.)

**Server side** — services return `Either<ApiError, T>`; `ApiError` is *semantic*, the route maps it to a status:

```kotlin
// :server:core:web — the ONE place ApiError meets HTTP
fun ApiError.toStatus() = when (this) {
    is Validation    -> HttpStatusCode.UnprocessableEntity
    Unauthorized     -> HttpStatusCode.Unauthorized
    is NotFound      -> HttpStatusCode.NotFound
    is Conflict      -> HttpStatusCode.Conflict
    is RateLimited   -> HttpStatusCode.TooManyRequests
    Internal, is UnknownApiError -> HttpStatusCode.InternalServerError
    else             -> HttpStatusCode.BadRequest
}
suspend fun ApplicationCall.respondError(e: ApiError) = respond(e.toStatus(), ErrorEnvelope(e, requestId()))
```

`Either` for *expected* failures; **StatusPages only for unexpected exceptions** → log + generic `Internal`, never leak. `ApiError` is the **information-disclosure boundary** (no `UserNotFound` vs `WrongPassword` — both collapse to `Unauthorized`).

**Client side** — `ApiError` stays pure in `:shared` (it can't implement the client's `AppError` — umbrella law), so it is wrapped in a carrier that does, and rides the existing `AppError`/`DelegatingError`/`ErrorRenderer` pipeline to a localized string:

```kotlin
// :client:core:error — one new variant on the existing NetworkError
sealed interface NetworkError : AppError {
    data class Http(val code: Int) : NetworkError
    data class Connection(val cause: Throwable) : NetworkError
    data class Serialization(val cause: Throwable) : NetworkError
    data class Api(val error: ApiError) : NetworkError       // wraps the shared type
}
// executeSafe parses a 4xx ErrorEnvelope into NetworkError.Api(...)
```

> **⚠ Open / deferred:** whether each `:shared:<domain>` adds a sealed `XApiError : ApiError` grouping as a **client-side narrowing lens** (autocomplete + compile-forced exhaustive domain handling; common/unknown fall to `else`). **Interim stop-gap in effect:** domain errors are declared directly as `: ApiError` with no grouping; services return loose `Either<ApiError, T>`; the client matches `is X -> … ; else -> generic`. Adding the grouping later is purely additive and non-breaking. Tracked as a pin; revisit once the server shape exists. Full rationale + the loose-vs-sealed-vs-lens options are in [ADR-0005](docs/adr/0005-error-model.md).

---

## 6. Server internals

([ADR-0006](docs/adr/0006-server-public-impl-internals.md)) Mirrors the client's `public`/`impl` split so cross-domain boundaries are **compiler-enforced**, not conventional.

### Core modules (no god core)

| Module | `public` | `impl` |
|--------|----------|--------|
| `:server:core:database` | `dbTransaction` helper, `TableSet`, `DatabaseConfig` | Hikari + Flyway + Exposed wiring, DB `HealthIndicator` |
| `:server:core:web` | `RouteRegistrar` / `PluginInstaller` contracts; `ApiError→status`, `Either`-fold responder | base Ktor plugins, ContentNegotiation, `Json` provider |
| `:server:core:auth` | `Principal`, session-store interface, `authenticate{}` | Session validation, middleware, cache |
| `:server:core:observability` | `HealthIndicator` contract, correlation-id key | CallId + MDC logging, Micrometer/metrics, `/health` + `/metrics` routes |

> **Implementation note (Phase 2):** the original design left `:server:core:observability` `public`-less, but the public/impl structure law requires every `:impl` to expose a contract — and a `HealthIndicator` multibinding (each infra/domain module contributes one; `/health` aggregates the set) is a genuinely useful one, so observability gained a small `:public`. The self-registration contracts (`RouteRegistrar` / `PluginInstaller`) live in `:server:core:web:public`.

### A domain slice (`:server:feature:<domain>`)

| Module | Contents |
|--------|----------|
| `:public` | the `Service` interface + cross-module domain types |
| `:impl` | `ServiceImpl`, `Repository`, Exposed tables, routes, stateful validation, Metro bindings — organized as `route/` `service/` `data/` **packages** |

```kotlin
// :server:feature:notes:impl — route is dumb, service owns logic + the transaction
fun Route.notesRoutes(service: NotesService) = authenticate {
    post<NotesResource> {
        call.respondEither(service.create(call.principal().account, call.receive())) {
            call.respond(HttpStatusCode.Created, it)
        }
    }
}

class DefaultNotesService(private val repo: NotesRepository) : NotesService {
    override suspend fun create(account: AccountId, req: CreateNoteRequest): Either<ApiError, NoteResponse> =
        either {
            val text = NoteText.of(req.text).mapLeft { Validation(listOf(it)) }.bind()  // shared shape check
            newSuspendedTransaction {                                                    // service owns the tx
                val used = repo.byteTotal(account)
                ensure(used + text.value.length <= QUOTA) { NotesQuotaExceeded(QUOTA, used) }  // stateful
                repo.insert(account, text).toResponse()                                  // repo returns domain type
            }
        }
}
```

### Disciplines

- **Service owns the transaction** (`newSuspendedTransaction`); repositories assume an ambient one (fixes witchy's per-query `transaction { }`).
- **Repositories return domain types, never Exposed `ResultRow`** — Exposed stays inside `data/`; services unit-test against fake repos.
- **Cross-domain = service → service only** — depend on the other domain's `:public` (its Service interface), never its tables.

### Self-registration (`:server:app` stays thin)

Routes, Ktor plugins, and Exposed table sets self-register via Metro `@ContributesIntoSet`, exactly like the client's `implAggregator` — the `convention.impl-aggregator` plugin (umbrella-scoped: `:server:*:impl` → `:server:app`) puts every impl on the app's classpath so Metro can merge the contributions. Adding a domain touches **zero lines** in `:server:app`.

A route is a `RouteRegistrar` (a `fun interface` with an `Application` receiver) that takes its service via constructor injection and opens its own `routing { }` / `authenticate { }` — cleaner than passing a bare `Route.() -> Unit`, and confirmed against a real Metro-on-Ktor setup. Plugins are ordered `PluginInstaller`s.

```kotlin
// :server:feature:notes:impl
@Inject @ContributesIntoSet(AppScope::class)
class NotesRoutes(private val service: NotesService) : RouteRegistrar {
    override fun Application.register() = routing {
        authenticate { post<NotesResource> { /* call service, respondEither */ } }
    }
}
@ContributesIntoSet(AppScope::class) @Provides fun notesTables(): TableSet = TableSet(Notes)
@ContributesIntoSet(AppScope::class) @Provides fun notesErrors(): SerializersModule = notesErrorModule
```

`main()` builds the Metro graph, runs `databaseBootstrap.start()` (Flyway migrate → Exposed connect), then installs the multibound `Set<PluginInstaller>` (sorted) and `Set<RouteRegistrar>` before starting Netty.

---

## 7. Persistence & migrations

([ADR-0007](docs/adr/0007-persistence-and-migrations.md)) Exposed + Postgres + HikariCP + Flyway.

- **Flyway SQL = DB source of truth; Exposed `Table` objects = code source of truth.**
- **Drift test** (Testcontainers): apply all migrations to a throwaway DB, fail the build if `MigrationUtils` reports a diff vs the Exposed schema (collected from the `@ContributesIntoSet` `TableSet` multibinding). "Forgot a migration" → red build.
- Schema **split per domain** (no single `Schema.kt`).
- Migrations **timestamp-versioned** (`V20260530__add_notes.sql`), **co-located** in each domain `:impl`'s resources; Flyway scans all locations. (Timestamps avoid the single global integer sequence colliding with per-domain co-location.)

---

## 8. Dependency injection

([ADR-0008](docs/adr/0008-metro-di-everywhere.md)) **Metro everywhere** — the same compile-time KSP DI as the client, one mental model across the monorepo. `:server:app` defines a `@DependencyGraph(AppScope)` server graph (analogous to the client's `JvmAppGraph`), exposing the assembled `Set<RouteRegistration>`, `Set<TableSet>`, `Set<SerializersModule>`, and the configured services as entry points. `main()` creates the graph, runs Flyway, installs Ktor, and starts Netty.

---

## 9. Auth

([ADR-0009](docs/adr/0009-auth-credential-session-seam.md)) Separate the **Credential module** (how you prove identity — swappable) from the **session/authorization infra** (invariant).

| Concern | Where |
|---------|-------|
| `Principal`, `Authentication` middleware, `authenticate{}`, session issuance/revocation, store interface | `:server:core:auth` (invariant) |
| Verify a credential → issue a Session (default: email + Argon2id) | `:server:feature:auth` (swappable) |

**Default scheme:** opaque **Session** token + cached server-side store (Postgres `sessions` table + Caffeine cache). Revocation = row delete. **Owned in-server**, no external provider.

Baked-in defaults (never hand-rolled per app): **Argon2id** hashing; **platform-secure client token storage** (Keychain / Keystore-backed / encrypted — *not* plain DataStore); global **401 → clear session → Login** interceptor on the client, wired into the existing Splash → Login → Main flow. witchy's device-key auth fits this as "just another Credential module."

---

## 10. Observability

Baseline (no standalone ADR — it's the obvious baseline):

- **Structured logging with a request/correlation ID in MDC** (replaces basic CallLogging); the same `requestId` appears in `ErrorEnvelope`.
- **Ktor MicrometerMetrics → `/metrics`** Prometheus endpoint.
- **Health check** endpoint (DB + version), retained from witchy.
- OpenTelemetry tracing is **opt-in later**, not baseline.

---

## 11. Scale posture

([ADR-0010](docs/adr/0010-scale-posture.md)) The template targets **"don't foreclose,"** not "scale now."

1. **Per-instance state behind an interface, tolerably-stale default** — session cache short TTL (≈30–60s), Redis swappable later. Rate limiting is per-node (note global needs a shared backing).
2. **Stateless app** — opaque-session-via-DB + blobs via a `BlobStore` interface, **never local disk**.
3. **Multi-node-safe background jobs from day one** — a scheduled-task primitive in `:server:core` using a Postgres advisory lock (`pg_try_advisory_lock`); jobs idempotent. Full deferred-job queue (outbox + worker) deferred.

Connection pool configurable; `instances × poolSize ≤ Postgres max_connections` (PgBouncer if ever hit). Result: going multi-node is config + an interface swap, never a rewrite, and never a silent correctness regression.

---

## 12. Local dev & config

### Infrastructure

A root `docker-compose.yml` brings up **Postgres + MinIO** (S3-compatible, so the `BlobStore` works locally exactly as in prod). Testcontainers remains for the test suite.

```
docker compose up -d            # Postgres + MinIO
./gradlew :server:app:run       # auto-runs Flyway; works with ZERO config via localhost defaults
```

Ktor dev-mode auto-reload is enabled for the server.

### Config — one typed, fail-fast `ServerConfig`

Loaded once at startup: **localhost dev defaults** (runs out-of-the-box) and **fail-fast** if a required prod secret is missing. `.env.example` is committed; `.env` is gitignored; prod overrides via real env vars (Railway). No scattered `System.getenv` calls.

### Client → server base URL — DI-provided `ApiConfig`

Build-variant driven (debug = localhost, release = prod), injected via Metro. Platform gotchas pre-solved so they're never re-learned:

| Platform | Host |
|----------|------|
| Android emulator | `10.0.2.2` (not `localhost`) — and debug builds allow cleartext HTTP to localhost |
| iOS simulator | `localhost` |
| Desktop | `localhost` |

An optional debug-only base-URL override allows pointing a dev build at staging.

### The "one whole thing" dev loop

Because client and server share `:shared` in one Gradle build, editing a DTO or route **recompiles both sides at once** — change the Contract and both ends show compile errors until fixed. No publishing, no version bumping, no codegen, no drift. This is the tangible payoff that tRPC approximates with types and we get with real shared code.

---

## 13. Convention plugins & enforcement

A `convention.server.*` family mirrors the client's plugins:

| Plugin | Purpose |
|--------|---------|
| `server.core.public` / `server.core.impl` | JVM server core module base |
| `server.feature.public` / `server.feature.impl` | JVM server domain module base (adds Metro, Arrow, Exposed in impl) |
| `server.app` | application plugin, main class, Ktor, distribution/Docker |

`module-graph-assert` + `module-structure-assert` are extended to police the umbrella dependency law, the `:shared` dep-surface, and server `public`/`impl` naming.

---

## 14. Deferred / future

Recorded so the boundary of "now" is explicit:

- **Real-time updates** (SSE/WS) — an enhancement layer over the REST baseline with graceful degradation.
- **kotlinx-rpc** — rejected (no wire-compat guarantees); see [ADR-0002](docs/adr/0002-rest-ktor-resources-contract.md).
- **Sealed per-domain `ApiError` grouping** — deferred; interim stop-gap in effect; see [ADR-0005](docs/adr/0005-error-model.md).
- **Redis shared cache**, **deferred-job queue** (outbox + worker), **read replicas**, **OpenTelemetry tracing** — all swap-in-when-needed per [ADR-0010](docs/adr/0010-scale-posture.md).
- **`/v2` API versioning** — only if additive evolution is ever genuinely exhausted.

---

## ADR index

See [`docs/adr/README.md`](docs/adr/README.md). Decisions: 0001 umbrellas · 0002 REST/Resources · 0003 share-the-wire · 0004 validation split · 0005 error model (partial) · 0006 server public/impl · 0007 persistence/migrations · 0008 Metro DI · 0009 auth · 0010 scale.
