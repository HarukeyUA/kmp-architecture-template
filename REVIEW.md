# Foundation Review — `feature/fullstack-server`

Pre-merge review of the fullstack extension. Goal: the template foundation is **stable and smell-free** before it becomes the thing every future app is copied from. Review target is the cumulative diff `main...HEAD` (final state), **not** commit-by-commit — several files were churned by later fix commits and only the end state ships.

## How to use this doc (two modes, in parallel)

1. **Manual pass** — work one Unit per sitting, tick the boxes, jot findings inline under each Unit.
2. **Agent cross-check** — launch a separate agent thread per Unit and have it review the same scope independently, then reconcile against your manual notes. Each Unit below is written to be pasted as-is into that thread.

**Cross-check prompt template** (fill in the Unit's scope + rubric):
> Independently review these files for a KMP fullstack *template* (foundation quality matters more than feature completeness). Scope: `<paths>`. Governing decisions: `<ADRs>`. Check for: `<rubric>`. Also answer the decision questions: `<questions>`. Report findings as: (1) correctness bug, (2) architecture/smell, (3) nit — and weight (2) heavily. Cite `file:line`. Don't fix anything; just report.

**Findings convention:** `(1)` correctness · `(2)` architecture/smell · `(3)` nit. For a template, **(2) is the priority** — a smell here gets copied forever.

**Global gate (do before Unit 1):** the branch must be green — full build + all tests + `assertModuleDependencies`. Reviewing for smells on a red branch wastes the pass. This is the author's own "must land green" bar per the plan.

**Order rationale:** dependency order (bottom-up). The seam and the enforcement layer sit at the bottom of the dep graph *and* are the highest-leverage to get right, so bottom-up naturally front-loads the important work while you're fresh.

**Treasure map:** the tail fix commits mark where the author already found problems — read these diffs and confirm each fix is complete and left no un-fixed sibling:
- `afe3527` Fix code-review findings: auth session reuse, notes quota race, error seam, quota query
- `2ff812d` server resource lifecycle + enhanced API error mapping
- `175edda` network error handling + centralized HttpClient content type
- `fea2302` distinctUntilChanged on isLoggedIn · `55b4ea9` logout navigation

**Out of scope:** Phase 7 (error grouping, real-time) — not in this branch.

---

## Unit 0 — Orientation (read, don't deep-review)

- [x] Re-read the ADRs as the **spec**, asking for each: *do I still believe this decision?* A correct implementation of a wrong ADR is still wrong.
- [x] Skim the commit log for the phase story (de-risk → restructure → skeleton → seam → tracer → replicate → harden).
- [x] Confirm branch is green (build + tests + module assertions).
- [x] Confirm the umbrella move is content-free — **do not read the ~182 moved files**:
  ```
  git diff -M main...HEAD --diff-filter=R --stat        # moves should show R100
  git diff -M main...HEAD --diff-filter=R --stat | grep -v 'R100' || echo "all pure"
  ```
  Spot-check only any non-R100 rename.

**Scope (read-only):** `docs/adr/0001`–`0010` + `docs/adr/README.md`, `ARCHITECTURE_SERVER.md`, `CONTEXT.md`, `docs/IMPLEMENTATION_PLAN.md`

**Decision questions:** Is "share the wire, never the domain" (0003) worth the double mapping? Is the open-polymorphic `ApiError` (0005) the right call vs sealed, given it's explicitly a stop-gap? Is the public/impl split (0006) pulling its weight on the server, or ceremony? Is Metro-everywhere (0008) justified server-side?

**Findings:**
-

---

## Unit 1 — Enforcement layer (build-logic + catalog)

The architecture *law* lives here. If these assertions are correct they guard the template against drift for free; a bug here = silent boundary erosion.

- [X] Convention plugins read cleanly, no copy-paste drift across the `server.*` family.
- [X] `AssertModuleDependenciesTask` actually rejects: `client ↔ server`, `:impl → :impl`, `shared → non-shared`, and the rationed `shared.contract` external-dep surface.
- [X] **Test the guard live:** add a deliberate `notes:impl → auth:impl` dep and confirm the assert fails; revert. (Memory says it does — verify it still does.)
- [X] Version catalog pins are current/stable and intentional (no surprise alphas).
- [X] `ImplAggregator` umbrella-scoping (`:server:*:impl → :server:app`) is correct and not leaking client impls into server or vice-versa.

**Governing ADRs:** 0001 (umbrella law), 0004 (validation-split enforced via dep rule), 0006 (public/impl), 0008 (Metro), 0010.

**Scope:**
```
build-logic/convention/build.gradle.kts
build-logic/convention/src/main/kotlin/AssertModuleDependenciesTask.kt   (org/example/project/)
build-logic/convention/src/main/kotlin/ServerAppConventionPlugin.kt
build-logic/convention/src/main/kotlin/ServerCore{Public,Impl}ConventionPlugin.kt
build-logic/convention/src/main/kotlin/ServerFeature{Public,Impl}ConventionPlugin.kt
build-logic/convention/src/main/kotlin/SharedContractConventionPlugin.kt
build-logic/convention/src/main/kotlin/ImplAggregatorConventionPlugin.kt
build-logic/convention/src/main/kotlin/ModuleGraphAssertConventionPlugin.kt
build-logic/convention/src/main/kotlin/org/example/project/ProjectExtensions.kt
build-logic/settings/src/main/kotlin/org/example/project/ImplAggregatorSettingsPlugin.kt
build-logic/settings/src/main/kotlin/org/example/project/ModuleStructureAssertSettingsPlugin.kt
settings.gradle.kts
gradle/libs.versions.toml
```

**Findings:**
-

---

## Unit 2 — The seam (`:shared:*`) — HIGHEST VALUE

The wire contract both sides bind to. Wire-compat mistakes are the most expensive to undo. Review `public`/contract intent before tests.

- [x] `common`: `ApiError` base + `UnknownApiError` fallback round-trips; the K/N "interface serializer rides inside `ErrorEnvelope`" gotcha is honoured; `SeamJson`/`buildSeamJson` is the single Json config used by both sides.
- [x] `@SerialName`s are namespaced (`auth.*`, `notes.*`) and **frozen by the golden tests** — confirm the golden tests would actually fail on a rename.
- [x] DTOs are minimal wire shapes — no domain model, no server-only or client-only concept leaking across.
- [x] Validation (`AuthValidation`, `NotesValidation`) is genuinely **pure & shared** (no platform/IO), and `NotesValidation` matches the server's quota constants (no doc/code drift).
- [x] `@Resource` routes are coherent, `/v1`-prefixed, and consumable by both ktor-server and ktor-client.

**Governing ADRs:** 0002 (resources contract), 0003 (share wire not domain), 0004 (validation split), 0005 (error model).

**Scope:**
```
shared/common/src/commonMain/.../common/{ApiError,ApiErrorSerialization,ErrorEnvelope,SeamJson}.kt
shared/auth/src/commonMain/.../auth/{AuthDtos,AuthErrors,AuthResource,AuthValidation}.kt
shared/notes/src/commonMain/.../notes/{NotesDtos,NotesErrors,NotesResource,NotesValidation}.kt
# tests (part of the contract guarantee):
shared/common/src/commonTest/.../{ApiErrorSerializationTest,ApiErrorSerialNameGoldenTest}.kt
shared/{auth,notes}/src/commonTest/.../{*ErrorGoldenTest,*ValidationTest}.kt
shared/{common,auth,notes}/build.gradle.kts
```
**Suggested agent:** `type-design-analyzer` on the DTO/error types.

**Findings:**
- All Dto-s must include explicit serial names for their fields
- Enforce route body/response via Endpoint class and .call/.serve client/server helpers

---

## Unit 3 — Server core (`:server:core:*`) — split into 5 short sittings

Largest unit. Do not do in one sitting. For each module: `public` surface minimal? `impl` hidden? resources released in `finally`?

### 3a — `web/` (request/response + error pipeline; pairs with Unit 2)
- [x] `ErrorResponder` (`toStatus`/`respondError`/`respondEither`) maps every `ApiError` variant; StatusPages safety-net never leaks a stack trace or swallows an error silently.
- [x] `Json` is folded from the multibound `Set<SerializersModule>` + base — same config as the client.
```
server/core/web/public/src/main/.../web/{ErrorResponder,PluginInstaller,RouteRegistrar}.kt
server/core/web/impl/src/main/.../web/{ContentNegotiation,Resources,StatusPages}PluginInstaller.kt
server/core/web/impl/src/main/.../web/WebProviders.kt
```

### 3b — `database/`
- [x] `Transactions` (`dbTransaction` helper) — repositories/stores open or join transactions, no leaked connection.
- [x] Hikari pool sizing configurable; Flyway runner; `InstantTimestampTz` TIMESTAMPTZ↔Instant column correct; `TableSet` multibinding contract.
```
server/core/database/public/src/main/.../database/{DatabaseConfig,InstantTimestampTz,TableSet,Transactions}.kt
server/core/database/impl/src/main/.../database/{DatabaseBootstrap,DatabaseHealthIndicator,DatabaseProviders,FlywayMigrator}.kt
```

### 3c — `auth/` (security-sensitive)
- [x] Opaque `Session` (no JWT), revoke-by-delete; `SessionStore` = Postgres + Caffeine short-TTL cache, cache can't outlive a revoked session beyond TTL.
- [x] `Authentication` middleware / `authenticatedRoutes{}` rejects missing/invalid/expired bearer correctly.
- [x] `ExpiredSessionSweeper` idempotent; sessions migration timestamps/indexes sane.
```
server/core/auth/public/src/main/.../auth/{Principal,Session,SessionStore,AuthRouting}.kt
server/core/auth/impl/src/main/.../auth/{DefaultSessionStore,SessionCache,SessionAuthPluginInstaller,ExpiredSessionSweeper,AuthServerBindings}.kt
server/core/auth/impl/src/main/.../auth/data/Sessions.kt
server/core/auth/impl/src/main/resources/db/migration/V20260530120000__create_sessions.sql
```

### 3d — `observability/` + `lifecycle/`
- [x] `/health` reflects real dependency health (DB indicator), not always-200; `/metrics` wired; call-id in MDC; `ServerResource` lifecycle closes in reverse order.
```
server/core/observability/public/src/main/.../observability/{Correlation,Health}.kt
server/core/observability/impl/src/main/.../observability/*.kt
server/core/lifecycle/public/src/main/.../lifecycle/ServerResource.kt
```

### 3e — `storage/` + `scheduler/` (subtlest correctness on the branch)
- [ ] `AdvisoryLockScheduler`: lock acquired on one pooled conn, **released in `finally`**; concurrent instances run the job once; boot-time rejects duplicate/colliding lock keys & non-positive interval.
- [ ] `BlobStore`: presigned-URL model, `require(len>0 && ttl.isPositive())`, S3Client built lazily (never a boot dep), `StorageConfig.toString()` redacts secrets.
```
server/core/scheduler/public/src/main/.../scheduler/ScheduledJob.kt
server/core/scheduler/impl/src/main/.../scheduler/{AdvisoryLockScheduler,SchedulerPluginInstaller,SchedulerProviders}.kt
server/core/scheduler/impl/src/test/.../AdvisoryLockSchedulerTest.kt
server/core/storage/public/src/main/.../storage/{BlobStore,StorageConfig}.kt
server/core/storage/impl/src/main/.../storage/{S3BlobStore,S3ClientFactory,S3ClientResource,StorageProviders}.kt
server/core/storage/impl/src/test/.../S3BlobStoreTest.kt
```

**Governing ADRs:** 0005, 0006, 0007, 0008, 0009 (3c), 0010 (3e).
**Suggested agent:** `silent-failure-hunter` on 3a + 3c + 3e.

**Findings:**
-

---

## Unit 4 — Server features (`:server:feature:*`)

Review `auth` **fully** (it's the pattern); read `notes` as a **replication check**.

- [ ] auth: route→service→repository layering; service has no transaction mechanics; repo returns domain types; `PasswordHasher` Argon2id params sane; routes self-register via bindings; accounts migration correct.
- [ ] notes: faithfully mirrors auth — any copy-paste drift or divergence is a smell. Quota race fix (`afe3527`) is correct (`used + len <= QUOTA` via `byteTotal`, no TOCTOU).
- [ ] **Cross-domain boundary:** `NotesService` calls only `auth:public` (`AuthService.me`), never the accounts table. Notes' `account_id` is a plain indexed UUID, no FK (domain-decoupled, by design).

**Governing ADRs:** 0006 (cross-domain), 0009 (credential/session), 0005 (domain errors).

**Scope:**
```
server/feature/auth/public/src/main/.../auth/AuthService.kt
server/feature/auth/impl/src/main/.../auth/{PasswordHasher,AuthFeatureBindings}.kt
server/feature/auth/impl/src/main/.../auth/{route/AuthRoutes,service/DefaultAuthService}.kt
server/feature/auth/impl/src/main/.../auth/data/{AccountRepository,Accounts}.kt
server/feature/auth/impl/src/main/resources/db/migration/V20260530110000__create_accounts.sql
server/feature/notes/public/src/main/.../notes/NotesService.kt
server/feature/notes/impl/src/main/.../notes/{NotesFeatureBindings,route/NotesRoutes,service/DefaultNotesService}.kt
server/feature/notes/impl/src/main/.../notes/data/{NoteRepository,Notes}.kt
server/feature/notes/impl/src/main/resources/db/migration/V20260530130000__create_notes.sql
```

**Findings:**
-

---

## Unit 5 — Server app glue + integration tests (`:server:app`)

- [ ] `ServerConfig`: localhost dev defaults, fail-fast on missing prod secrets, blank-treated-as-missing, secrets redacted in `toString()`.
- [ ] `ServerGraph`/`Main`: Metro graph wiring + boot sequence (graph → Flyway → install plugins → start Netty) is clean.
- [ ] **Verify the self-registration claim:** `:server:app` `src/main` gained ~zero domain-specific lines as auth+notes were added (routes/tables/errors self-register via multibindings).
- [ ] Integration tests assert the real guarantees: Auth gate (signup→/me→401→logout→revoked-401→dup→validation), Notes CRUD + cross-domain authorEmail + quota, MigrationDrift auto-covers all tables.

**Governing ADRs:** 0008 (graph), 0007 (flyway/drift), 0010.

**Scope:**
```
server/app/src/main/kotlin/.../server/{Main,ServerConfig,ServerGraph}.kt
server/app/src/main/resources/logback.xml
server/app/build.gradle.kts
server/app/src/test/kotlin/.../server/{AuthIntegrationTest,NotesIntegrationTest,MigrationDriftTest,TestConfig}.kt
```

**Findings:**
-

---

## Unit 6 — Client integration core (network / secure-storage / error)

Security-sensitive: token storage per platform + the 401→clear→Login path.

- [ ] `HttpClientGraph`: bearer pulled from secure store; `refreshTokens`/Auth plugin = global 401 → clear session → bounce to Login; centralized content-type (`175edda`).
- [ ] `SafeRequest`/`executeSafe`: Arrow `ensure` refactor reads cleanly; parses 4xx `ErrorEnvelope` into `NetworkError.Api`; no silent catch-all swallowing real failures.
- [ ] `ApiConfig` per-platform: `10.0.2.2` (Android emulator), iOS/JVM localhost, cleartext only in debug.
- [ ] `KSafeSecureSessionStore` uses OS keystore on each platform (Keychain/Keystore/encrypted) — **never DataStore**; `SecureSessionStore` interface clean.

**Governing ADRs:** 0009 (401, secure storage), 0005 (client error parse), 0002 (client resources).

**Scope:**
```
client/core/network/public/src/commonMain/.../network/{ApiConfig,SafeRequest}.kt
client/core/network/impl/src/commonMain/.../network/{HttpClientGraph,NetworkProviders}.kt
client/core/network/impl/src/{android,apple,jvm}Main/.../network/{ApiConfigGraph,HttpClientEngine}.kt
client/core/secure-storage/public/src/commonMain/.../secure/storage/SecureSessionStore.kt
client/core/secure-storage/impl/src/commonMain/.../secure/storage/KSafeSecureSessionStore.kt
client/core/secure-storage/impl/src/{android,apple,jvm}Main/.../secure/storage/SecureStorageGraph.kt
client/core/error/public/build.gradle.kts   # NetworkError.Api carrier (see core/ui/error in Unit 7)
```
**Suggested agent:** `silent-failure-hunter` on `SafeRequest`/`executeSafe` + the 401 interceptor.

**Findings:**
-

---

## Unit 7 — Client features (`:client:feature:auth` + `:client:feature:notes`)

- [ ] Repositories map **Wire→Domain** (`toModel`) — the wire type never leaks into UI; `AuthRepository`/`NotesRepository` public surfaces are clean.
- [ ] `DefaultLoginComponent`/`DefaultNotesComponent` (Molecule) state handling; `RootComponent` logout nav (`55b4ea9`) + `isLoggedIn` `distinctUntilChanged` (`fea2302`) correct.
- [ ] Search→Notes tab swap is complete — **no dangling `search` references** (deletions in Unit 8 list).
- [ ] `ApiErrorRenderer` renders localized strings for known variants; tests (MockEngine network tests, screenshot tests) assert real behavior.
- [ ] `core/ui/error` changes (`ApiErrorRenderer`, `CompositeErrorRenderer`, `NetworkErrorRenderer`, `LocalErrorRenderer`) + new `strings.xml` entries.

**Governing ADRs:** 0003 (wire→domain), 0002.

**Scope:**
```
client/feature/auth/public/.../auth/AuthRepository.kt
client/feature/auth/impl/src/commonMain/.../auth/{AuthClientBindings,DefaultLoginComponent,LoginScreen,data/AuthRepositoryImpl}.kt
client/feature/auth/impl/src/commonTest/.../auth/{AuthRepositoryNetworkTest,DefaultLoginComponentTest,Fake*,InMemory*}.kt
client/feature/notes/public/.../notes/{Note,NotesComponent,NotesRepository,NotesScreen}.kt
client/feature/notes/impl/src/commonMain/.../notes/{DefaultNotesComponent,NotesClientBindings,NotesScreen,data/NotesRepositoryImpl}.kt
client/feature/notes/impl/src/commonTest/.../notes/NotesRepositoryNetworkTest.kt
client/core/ui/public/src/commonMain/.../core/ui/error/*.kt + composeResources/values/strings.xml
client/composeApp/.../RootComponent.kt
client/feature/main/impl/.../main/presentation/{DefaultMainComponent,MainScreen}.kt   # Tab.Search→Notes
client/feature/user-data/.../user/data/{UserRepository,UserRepositoryImpl,FakeUserRepository}.kt
```

**Findings:**
-

---

## Unit 8 — Ops/deploy + final cross-cutting sweep

- [ ] `Dockerfile` (installDist → JRE image, no in-container build), `.dockerignore`, `docker-compose.yml` (Postgres + MinIO + bucket init), `.env.example` complete & matches `ServerConfig`.
- [ ] CI `pr-verification.yml` still covers the new modules.
- [ ] **Doc honesty:** every `ARCHITECTURE_SERVER.md` section flipped to "implemented" actually is; `CONTEXT.md` glossary matches the code; `ARCHITECTURE.md`/`README.md` module paths updated to `:client:*`.
- [ ] No dangling references to the deleted `search` feature anywhere (settings, graphs, nav).
- [ ] Final naming-consistency sweep across umbrellas.

**Governing ADRs:** 0010.

**Scope:**
```
Dockerfile  .dockerignore  docker-compose.yml  .env.example
.github/workflows/pr-verification.yml
ARCHITECTURE_SERVER.md  ARCHITECTURE.md  CONTEXT.md  README.md
# confirm fully removed:
feature/search/**  (and old core/feature paths replaced by client/*)
```

**Findings:**
-

---

## Sign-off

- [ ] All units reviewed; (1)/(2) findings triaged (fixed, ticketed, or accepted-with-rationale).
- [ ] Branch green after any fixes.
- [ ] ADR-0005 stop-gap caveat still accurately documented as deferred to Phase 7.
