# Template Changelog

Architecture-relevant changes to this template, for downstream projects that started from a copy
of it. Dependency bumps are not recorded — Renovate handles those. To adopt a change into a
project without shared git history, add this repo as a remote and diff the paths mentioned here
(`git remote add template <url>; git diff template/main -- <path>`).

## 2026-07

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
- **Server test harness refinements** — single-boot `serverTest(...)` seam with
  `TestPostgres`/`TestMinio`, published seam fakes (`FakeBlobStore`, `FakeTransactionRunner`).
- **Robots E2E framework** — `:client:core:robots` (`Robot`/`Wait` base), per-feature `:robots`
  modules with a robots-aggregator convention plugin, and the client↔server
  `:client:core:integration-testing` harness.
- **Design system components** — `AppTopBar` + scroll behavior, `AppListScaffold`,
  `AdaptiveBottomSheetOrDialog`, `ThemeMode`/`DynamicColor` theming (MaterialKolor).
- **Network hardening** — Kermit logging in the client `HttpClient`, `UnauthorizedPolicy` with
  rejected-credential (401) reporting interceptor, cold-start-sized timeouts,
  `ApiConfig`/`ServerOrigin` split.
- **`AppDispatchers` interface style** — witchy replaced dispatcher qualifiers with an
  `AppDispatchers` interface plus `TestAppDispatchers`; adopting it is a design migration across DI
  and the detekt `InjectDispatcher` config, to be decided deliberately.
- **Smaller modules** — `:client:core:datetime`, `:client:core:buildinfo` (BuildKonfig
  `Environment`), Coil convention plugin, AboutLibraries, path-filtered server CI, release
  workflow.

Intentionally not ported: `UniqueErrorCodesTest` (only meaningful under witchy's sealed-lens
serialization, ADR-0018 — this template's polymorphic seam `Json` already fails fast on duplicate
wire codes and pins them with golden tests), and the whole E2EE sync/crypto domain (product
architecture, not template infrastructure).
