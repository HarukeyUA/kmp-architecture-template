# Implementation Plan — Fullstack Extension

Sequenced plan to build the server + shared seam described in [`ARCHITECTURE_SERVER.md`](../ARCHITECTURE_SERVER.md). Decisions are in [`docs/adr/`](adr/README.md); vocabulary in [`CONTEXT.md`](../CONTEXT.md).

**Sequencing logic:** de-risk the unknown mechanisms *first* (cheap to fail early), land the large structural refactor *in isolation* (no behaviour change, easy to verify), then drive **one thin vertical slice through the entire stack** as a tracer bullet before investing in breadth, then harden and deploy. Each phase is a self-contained PR that must land green — full build, all tests, and the module assertions — before the next begins. As sections land, flip their status in `ARCHITECTURE_SERVER.md` from "target" to "implemented."

| Phase | Outcome | Implements |
|------:|---------|-----------|
| 0 | Risky mechanisms proven in scratch modules | — |
| 1 | Client modules under `:client:*` umbrella, still green | ADR-0001 |
| 2 | `:server:app` boots: health, metrics, DB, config | 0006, 0007, 0008, 0010 |
| 3 | Seam + error pipeline wired end-to-end (stop-gap) | 0003, 0004, 0005 |
| 4 | **Tracer bullet** — auth slice works client↔server | 0002, 0009 + all above |
| 5 | Second domain replicates the pattern cheaply | 0006 (cross-domain) |
| 6 | Hardening: BlobStore, job scheduler, deploy | 0010 |
| 7 | Revisit deferred: error grouping, real-time | 0005, 0002 |

---

## Phase 0 — De-risk spikes (throwaway)

**Goal:** prove the three mechanisms we flagged "verify at implementation" *before* building convention plugins and module structure around them. Time-boxed, throwaway (or folded into Phase 2 if confident).

**Steps**
- **Metro on plain-JVM/Ktor** — a scratch JVM module with a `@DependencyGraph` providing a value into a Ktor route. Confirm the ergonomics and KSP setup. (ADR-0008)
- **Polymorphic `ApiError` aggregation** — an open `interface ApiError`, two subtypes in two modules each exposing a `SerializersModule`, combined via `SerializersModule.plus`, with `polymorphicDefaultDeserializer` → `UnknownApiError`. Assert: known codes round-trip, an unknown code lands on `UnknownApiError`, a duplicate `@SerialName` throws at module build. (ADR-0005)
- **Flyway + Testcontainers drift test** — apply a migration to a throwaway Postgres, diff `MigrationUtils` against an Exposed table; assert a deliberate mismatch fails. (ADR-0007)
- **Ktor Resources shared** — one `@Resource` consumed by both `ktor-server-resources` (`get<R>`) and `ktor-client-resources` (`client.get(R(...))`). (ADR-0002)

**Done when:** each mechanism demonstrably works in isolation; any ergonomic surprises are noted and reflected back into the ADRs/architecture doc before Phase 1.

---

## Phase 1 — Umbrella migration (structural, no behaviour change)

**Goal:** move today's client modules under `:client:*` so the three-umbrella law has a home. Pure refactor — zero functional change. (ADR-0001)

**Steps**
- Move `:core:*` → `:client:core:*` and `:feature:*` → `:client:feature:*` (directories + `settings.gradle.kts` includes + every `project(":...")` reference + iOS framework export paths).
- Update the settings/build-logic plugins that hard-code paths: `implAggregator`, `module-structure-assert`, `assertModuleDependencies`.
- Update `ARCHITECTURE.md` module paths to the new structure (and drop the "until that migration lands" caveat once done).

**Done when:** `./gradlew build` + `assertModuleDependencies` green; all unit + screenshot tests pass; Android, iOS, and desktop apps still launch and behave identically. Land as its own PR — never mix this mechanical move with new code.

**Depends on:** Phase 0 (low coupling; can run in parallel if confident).

---

## Phase 2 — Server skeleton (it boots)

**Goal:** a `:server:app` that starts, talks to Postgres, and is observable — no domains yet. (ADR-0006/0007/0008/0010)

**Steps**
- Version catalog: Ktor server (+ Netty, content-negotiation, status-pages, auth, call-id/MDC), Exposed (core/jdbc/datetime), HikariCP, Postgres driver, Flyway, Caffeine, Argon2, logback, Micrometer/Prometheus, Testcontainers.
- `convention.server.*` plugin family: `server.core.public|impl`, `server.feature.public|impl`, `server.app` (application plugin, main class, distribution).
- Modules: `:server:core:database:public|impl`, `:server:core:web:public|impl`, `:server:core:observability:impl`, `:server:app`.
- `:server:app`: Metro `@DependencyGraph(AppScope)`, `main()` → build graph → run Flyway → install Ktor (ContentNegotiation, StatusPages safety-net, structured logging + correlation id in MDC, MicrometerMetrics → `/metrics`, `/health`) → start Netty.
- `:server:core:database`: Hikari pool (configurable size), Flyway runner, Exposed connect, the `dbTransaction` helper, the `TableSet` multibinding contract.
- Typed `ServerConfig`: localhost dev defaults + fail-fast on missing prod secrets; `.env.example` committed, `.env` gitignored.
- Root `docker-compose.yml`: Postgres + MinIO.
- The drift-test harness wired (empty schema passes).
- Extend `module-graph-assert` + `module-structure-assert` for the umbrella law, `:shared` dep-surface, and server `public`/`impl` naming.

**Done when:** `docker compose up -d` + `./gradlew :server:app:run` boots; `/health` and `/metrics` respond; Flyway runs (no migrations yet); drift test + module assertions green.

**Depends on:** Phase 1.

---

## Phase 3 — Seam + error pipeline (stop-gap)

**Goal:** the shared contract scaffolding and the full error round-trip, before any domain exists. (ADR-0003/0004/0005)

**Steps**
- `convention.shared.contract` plugin (rationed deps: kotlinx.serialization, ktor-resources, arrow-core, kotlinx.datetime) + the `:shared`-only dependency rule.
- `:shared:common`: open `ApiError` base + cross-cutting variants + `UnknownApiError` default deserializer + base `SerializersModule`; `ErrorEnvelope`; `ValidationError`/`FieldError`; shared value types.
- `:server:core:web`: `ApiError → HttpStatusCode` map, `respondEither` responder, and the `@Provides Json` that folds the multibound `Set<SerializersModule>` + base. Route StatusPages through `respondError`.
- Client: `NetworkError.Api(apiError)` carrier in `:client:core:error`; `executeSafe` parses a 4xx `ErrorEnvelope`; an `ErrorRenderer` for `ApiError` variants; client `Json` from the same multibinding.
- Golden-set freeze test scaffolding for cross-cutting error `@SerialName`s.

**Done when:** a round-trip test serializes an `ApiError` through `ErrorEnvelope` on both client and server modules; an unknown code → `UnknownApiError`; the client renders a localized string for a known variant. **Stop-gap note:** domain errors will be plain `: ApiError` (no sealed grouping) per ADR-0005.

**Depends on:** Phase 2.

---

## Phase 4 — Tracer bullet: the auth slice (end-to-end)

**Goal:** drive one slice through the *entire* stack. Auth is the right first slice — everything else needs it, and it exercises the credential/session seam, persistence, the error model, DI, and the client's 401→Login flow at once. (ADR-0002/0009)

**Steps**
- `:server:core:auth`: `Principal`, session-store interface, `Authentication` middleware + `authenticate{}`, session issuance/revocation, Caffeine cache (short TTL, behind the interface).
- `:shared:auth`: `@Resource` routes (signup/login/logout) under `/v1`, request/response DTOs, shared shape validation (email/password rules), auth `ApiError`s + their `SerializersModule`.
- `:server:feature:auth:public|impl`: Credential module (email + **Argon2id**) issuing a Session; `sessions` table + timestamp migration; service (orchestrates use case) + transaction-safe repository/store operations (return domain types); routes self-register via `@ContributesIntoSet`.
- Client: point the existing Splash → Login → Main flow at the real server; **platform-secure token storage** (Keychain / Keystore-backed / encrypted, not DataStore); global **401 → clear session → Login** interceptor; DI `ApiConfig` (base URL + `10.0.2.2` / cleartext-debug / iOS-localhost).
- Tests: service unit test (fake repo); Testcontainers integration test for the routes; drift test now covers `sessions`; golden test covers auth errors.

**Done when (the validation gate):** locally, sign up → log in → call an authenticated endpoint → log out; revoke a session server-side → next call returns 401 → client bounces to Login. The full architecture — seam, contract, validation, error model, auth, persistence, DI, observability, the one-build dev loop — is proven.

**Depends on:** Phase 3.

---

## Phase 5 — Second domain (prove cheap replication + cross-domain)

**Goal:** confirm the pattern replicates with near-zero ceremony and that the cross-domain boundary holds. (ADR-0006)

**Steps**
- Add a simple non-auth domain (e.g. a small CRUD domain): `:shared:<domain>`, `:server:feature:<domain>:public|impl`, and a client feature consuming it via `toModel()` mapping.
- Include one **cross-domain service→service call** (depend on another domain's `:public`, never its tables) to exercise the structural boundary.
- Confirm `:server:app` is untouched (routes/tables/error modules self-register); the drift and golden tests extend automatically via the multibindings.

**Done when:** the domain works end-to-end; `:server:app` has zero new lines; module assertions reject a deliberate attempt to reach another domain's `:impl`.

**Depends on:** Phase 4.

---

## Phase 6 — Hardening & deploy

**Goal:** the not-foreclose primitives and a real deployment. (ADR-0010)

**Steps**
- `BlobStore` interface + S3/MinIO implementation (object storage; never local disk); local dev uses the compose MinIO.
- The advisory-lock scheduled-task primitive in `:server:core` (`pg_try_advisory_lock`) + one example idempotent job.
- Deploy: Dockerfile (`installDist` → JRE image), chosen target (e.g. Railway) config, prod env wiring, `/metrics` scrape.
- Connection-pool sizing note honoured (`instances × pool ≤ max_connections`).
- Docs pass: flip `ARCHITECTURE_SERVER.md` sections to "implemented"; remove resolved stop-gap caveats.

**Done when:** a blob uploads/downloads via object storage end-to-end; the scheduled job runs exactly once across two local instances; the server deploys and serves health/metrics in the target environment.

**Depends on:** Phase 4 (Phase 5 not strictly required).

---

## Phase 7 — Revisit the deferred (when ready)

**Goal:** with the server shape in hand, resolve what was deferred. (ADR-0005, ADR-0002)

**Steps**
- **Error grouping** (pin / task #1): experiment with the sealed per-domain `XApiError : ApiError` client-side narrowing lens against the now-real domains; it's purely additive over the stop-gap. Update ADR-0005 to "Accepted" with the chosen approach.
- **Real-time**: add an SSE (or WS) enhancement channel over the REST baseline with graceful degradation, when an app needs it.

**Done when:** ADR-0005 is no longer "partial"; (real-time only if/when an app requires it).

**Depends on:** Phases 4–5 (a real server shape to experiment against).
