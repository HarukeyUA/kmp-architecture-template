# Template Changelog

Architecture-relevant changes to this template, for downstream projects that started from a copy
of it. Dependency bumps are not recorded — Renovate handles those. To adopt a change into a
project without shared git history, add this repo as a remote and diff the paths mentioned here
(`git remote add template <url>; git diff template/main -- <path>`).

## 2026-07

- **Client↔server integration tests in a test-only `:integration` umbrella.** Witchy runs its
  repository-vs-real-server tests from client test source sets that depend on the server harness;
  this template instead adds a fourth top-level umbrella: `:integration:*` may depend on
  everything, and nothing may depend on it (enforced in `assertModuleDependencies`), so `:client`
  never carries a `:server` dependency — not even a test one. `:integration:client-server` drives
  the real client repositories over `serverTest`'s in-process transport: `clientStack()` wires
  `AuthRepositoryImpl`/`NotesRepositoryImpl` like the app graph (seam `Json`, `sessionBearer`,
  in-memory session store) with the `testApplication` client as the wire into the booted
  `:server:*` stack. `NotesRepositoryIntegrationTest` pins the seam pair end-to-end: signup →
  create → list (cross-domain author email) → delete, plus the server's `notes.quota_exceeded`
  narrowing into the client's typed `CallFailure.Declared` through the same sealed lens. The
  tradeoff vs witchy is colocation: these suites live beside the seam they prove, not inside the
  feature (whose `commonTest` keeps its MockEngine tests). The client-only variant strips the
  umbrella wholesale.
- **Client network hardening (ported from witchy-notes; no runtime override, no 401 reporter).**
  The client `HttpClient` gains cold-start-sized timeouts (60s request / 30s connect / 60s socket —
  sized to outlast a sleeping PaaS instance's wake-up) and DEV-only wire logging through Kermit
  (tag `ktor`): the Logging plugin is not installed at all in PROD, and the Authorization header is
  redacted even in DEV. Witchy's `UnauthorizedPolicy` + rejected-credential (401) reporting
  interceptor was deliberately **not** ported: it exists because witchy's device tokens have no
  refresh path, so any bearer 401 is a final credential rejection there — in this template a
  bearer 401 is routine access-token expiry, and porting the reporter would log users out on every
  expiry. The equivalent behavior already lives at the right point (`sessionBearer` clears the
  session only when the *refresh endpoint* declares `auth.session_expired`) and is now pinned by
  `SessionBearerAuthTest` (MockEngine): refresh-mints-and-retries, session-expired-clears,
  ambient-failure-keeps-session. The runtime dev-server override remains unported (needs settings
  storage).
- **Server test harness (ported from witchy-notes).** New `:server:testing` module — the single
  way a suite boots the `:server:*` stack. `serverTest { client -> … }` resets the process-wide,
  once-migrated Testcontainers Postgres (`TestPostgres`, ambient-Exposed rebind + catalog-derived
  truncation), builds a fresh `ServerGraph`, installs it into `testApplication`, hands the block
  the seam-framed client, and closes the graph's pool in a `finally`. Overridable
  `storageConfig`/`webLimitsConfig`/`jwtConfig`; `serverGraphTest` exposes the graph for routeless
  service calls; `TestMinio` for presigned-URL suites; fixtures `signupViaApi` +
  `HttpResponse.decodedError()` (moved out of `:server:app` tests). The three integration suites
  (auth, notes, web hardening) migrated: ~30 lines of per-test graph boilerplate each became one
  `serverTest` line, and only the first suite pays the container+Flyway cost (notes: 0.2s,
  hardening: 0.5s). Also new: `convention.server.testing` and `:server:core:storage:testing` with
  `FakeBlobStore` for blob-owning domains. Witchy's `FakeTransactionRunner` was not ported — the
  template has no `TransactionRunner` abstraction (repositories own transactions via
  `dbTransaction`, ADR-0006), so there is no interface to fake. `MigrationDriftTest` deliberately
  keeps its own throwaway container (it needs a virgin database).
- **Robots E2E framework (ported from witchy-notes; no headless client).** Instrumented Android
  E2E infrastructure driving the real app against the real dev server. New `:client:core:robots`
  (`Robot`/`Wait` base, host-agnostic via `SemanticsNodeInteractionsProvider`) and per-feature
  `:robots` modules (auth/main/notes) reading test tags declared next to the screens in their
  sibling `:impl`; wired into `:client:androidApp`'s `androidTest` automatically by the new
  robots-aggregator settings+convention plugin pair (same pattern as the impl aggregator). The
  suite runs under the Test Orchestrator (`clearPackageData` — isolation by account-per-test,
  `animationsDisabled`), is dev-flavor-only (`beforeVariants` disables prod androidTest), captures
  failure screenshots into test-services storage, and health-gates on the dev server
  (`DevServerHealthRule`). `scripts/e2e-android.sh` guards the run (server health, adb, device)
  and bakes `DEV_SERVER_HOST=10.0.2.2`. One example flow, `NoteRoundTripTest`: signup → create
  note → note card + server-resolved author email appear. Module rules extended: `robots` leaf
  name, `:robots` → sibling `:impl` + `:client:core:robots` only. CI compiles the suite APK
  (`assembleDevDebugAndroidTest`) in both variants; the client-only variant keeps the robots
  infrastructure and swaps in an offline `LoginFlowTest` (fake auth), stripping the
  server-coupled pieces. Witchy's `:client:core:integration-testing` headless-client harness was
  deliberately not ported — it exists for multi-device sync convergence, and the template is one
  server/one client.
- **Per-target prod/dev environments (ported from witchy-notes; no runtime override).** Every
  client target now decides its backend at build time — Android `prod`/`dev` product flavors
  (distinct `applicationId`, " Dev" label, dev-only cleartext allowance, baked
  `BuildConfig.DEV_SERVER_HOST`), iOS `Debug-dev`/`Release-dev` Xcode configurations + `iosApp-dev`
  scheme (`DEV` compilation condition, `.dev` bundle id, `DevServerHost` via `Info-dev.plist` +
  `Generated.xcconfig`), desktop `-PappEnv=dev` (baked `app.env` launcher property) — and injects
  `Environment` + platform `ApiConfigDefaults` at graph creation; `resolveApiConfig` in
  `:client:composeApp` is the single place a base URL enters the app. Dev builds bake the build
  machine's address (Android: LAN IP auto-detect or `DEV_SERVER_HOST`; iOS: Bonjour name) so
  physical devices reach the local server with zero setup, validated by `normalizeOriginOrNull`
  (new, `:client:core:network:public`) with loopback fallback. `PROD_SERVER_BASE_URL` is a
  committed placeholder. New `:client:core:buildinfo:public` module holds `Environment`. The
  hardcoded per-platform `ApiConfigGraph` providers are gone. Unlike witchy-notes there is no
  runtime dev-server override — that needs the client settings storage (see pending ports).
- **One-command local dev stack (ported from witchy-notes).** `scripts/dev-stack.sh` boots
  Postgres + MinIO + `:server:app` with port pre-flight (names the squatter instead of compose's
  opaque failure), auto-created `.env`, health-waited containers, and env export; `--down` keeps
  data, `--nuke` drops it, `--lan` serves on the LAN IP so physical devices — and the presigned
  blob URLs the server mints (SigV4 signs the host) — work off-machine. New `LOG_LEVEL` logback
  knob (the script defaults it to DEBUG; framework loggers stay pinned at INFO). README gained the
  full-stack local loop (server + each client platform); the client-only variant now overlays its
  own README instead of inheriting the full-stack one.
- **Sealed-lens error serialization (ADR-0012, ported from witchy-notes).** Errors cross the seam
  through compiler-generated sealed serializers instead of a registered polymorphic module, and
  every `ApiError` variant declares its own HTTP status. Deleted wholesale: per-domain
  `SerializersModule`s and their client/server `@Provides` bindings, the `ApiErrorStatusMapper`
  multibinding, and `buildSeamJson` (now one static `seamJson` on both ends). Adding a Declared
  error is now the variant declaration plus one deliberate freeze-golden edit. New guarantees as
  tests: per-domain `*DeclaredErrorFreezeTest` pins `operation → {code → status}`, and
  `UniqueErrorCodesTest` in `:server:app` pins global code uniqueness. Wire format unchanged.
- **Merged the full-stack prototype back into the template.** The repo now contains the `:server:*`
  umbrella (Ktor + Exposed + Flyway + Metro composition root, JWT/session auth, S3 blob store,
  advisory-lock scheduler, isolated metrics port) and the `:shared:*` seam (typed
  `Endpoint<R, Req, Res, Err>` contracts, polymorphic `ApiError`, Declared/Ambient error model per
  ADR-0011, shared shape validation). The client gained the `CallFailure` typed-error channel,
  secure session storage (KSafe), and the seam-proving notes feature; the search sample feature was
  removed. All client code — including the ported auth and notes features — uses the
  eventSink-in-state event style.
- **Client-only variant tooling.** `scripts/make-client-only.sh` + `scripts/client-only/` derive
  the client-only template from this tree; the `client-only-variant` CI job keeps it building.
- **Server tests in CI.** The plain-JVM server modules' `test` tasks (Testcontainers integration
  suites) run in a dedicated CI job; they were not covered by `jvmTest`.
- **`restorableChildStack`** in `:client:core:navigation:public` (ported from a downstream
  project): a `childStack` wrapper that discards saved stacks containing `NotRestorable`
  configurations instead of recreating screens whose inputs did not survive process death.

## Pending ports (from witchy-notes-kmp)

Generic infrastructure proven downstream, to be ported in follow-up changes. Sources live in
`/projects/personal/witchy-notes-kmp` (package `com.harukeyua.witchynotes`).

- **Client persistence core** — `:client:core:database`: Room 3 KMP with
  `TransactionRunner`/`RaiseableTransaction`, `DatabaseWiper`, DAO fakes in a `:testing` module;
  optional SQLCipher encryption at rest.
- **Design system components** — `AppTopBar` + scroll behavior, `AppListScaffold`,
  `AdaptiveBottomSheetOrDialog`, `ThemeMode`/`DynamicColor` theming (MaterialKolor).
- **Runtime dev-server override** — the developer-settings screen reading a persisted URL in
  `resolveApiConfig` (DEV only) needs client settings storage first (DataStore local-storage or
  the Room persistence core). The rest of witchy's network hardening has been adopted or
  deliberately skipped — see the 2026-07 entry.
- **`AppDispatchers` interface style** — witchy replaced dispatcher qualifiers with an
  `AppDispatchers` interface plus `TestAppDispatchers`; adopting it is a design migration across DI
  and the detekt `InjectDispatcher` config, to be decided deliberately.
- **Smaller modules** — `:client:core:datetime`, BuildKonfig version/commit fields in
  `:client:core:buildinfo` (the module and `Environment` are already adopted), Coil convention
  plugin, AboutLibraries, path-filtered server CI, release workflow.

Intentionally not ported: the whole E2EE sync/crypto domain (product architecture, not template
infrastructure). Witchy's sealed-lens error serialization and `UniqueErrorCodesTest` have since
been adopted (see the 2026-07 entry above).
