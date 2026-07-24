# KMP Architecture Template

An opinionated Kotlin Multiplatform application template for building Compose Multiplatform apps with a strongly
modular architecture, shared component logic, typed failures, and process-death-safe state.

The project targets Android, iOS, and desktop from one shared Compose codebase. The template includes navigation, lifecycle/state bridges,
DI wiring, module graph enforcement, test utilities, screenshot testing, and scripts for renaming the
project or generating new feature slices.

## TL;DR

- **Molecule + Decompose state production**: `MoleculeComponent` is the default stateful component
  primitive. Components produce `StateFlow` UI state from a `@Composable produceState()` function,
  receive user actions through Circuit-style `eventSink` lambdas carried by the state itself, and
  live inside Decompose/Essenty component contexts.
- **Process-death-safe state**: `MoleculeComponent` bridges Essenty `StateKeeper` into Compose's
  `SaveableStateRegistry`, so `rememberSaveable` and `rememberSerializable` work inside Molecule
  state production. This applies to Android process death and is also wired for iOS restoration.
- **Lifecycle-aware Compose APIs outside UI**: Essenty lifecycle is exposed as an AndroidX
  `LifecycleOwner`, which lets component's molecule state production use lifecycle APIs such as
  `collectAsStateWithLifecycle`, `LifecycleEventEffect`, and related lifecycle-aware Compose helpers.
- **Typed errors with human-readable rendering**: recoverable failures are modeled with Arrow
  `Either<AppError, T>` instead of exceptions. Feature errors can wrap lower-level errors, while the
  renderer system maps typed errors to localized UI messages without repeating generic handling.
- **Hierarchical snackbar dispatch**: every `AppComponentContext` carries a snackbar handler. Child
  components can emit snackbar messages and they bubble to the nearest registered host, so deeply
  nested features do not need to know where the snackbar is rendered.
- **Public/impl module boundaries**: feature and core modules are split into `:public`, `:impl`, and
  optional `:testing` modules. Public modules expose contracts; impl modules contain Metro bindings,
  component implementations, and screens.
- **Module aggregation and enforcement**: `:client:composeApp` automatically aggregates every `:impl`
  module for app wiring, while build logic enforces dependency rules during settings evaluation and
  `check`. Public modules can only depend on public modules, impl modules cannot depend on other impl
  modules, testing modules depend only on their sibling public module, and core cannot depend on
  features.
- **Navigation without leaking child types**: stack components use Decompose `childStack` with
  serializable configurations. `ScreenChild` lets a parent render child screens without exposing each
  child component type in public APIs.

For the full rationale and conventions, see [ARCHITECTURE.md](ARCHITECTURE.md).

## Project Layout

```text
client/            Compose Multiplatform app — the :client:* umbrella
  androidApp/      Android entry point
  desktopApp/      JVM Desktop entry point
  composeApp/      Shared app, app graph, and root navigation
  core/            Reusable architecture, UI, dispatchers, networking, storage, and testing modules
  feature/         Feature slices using public/impl/testing module boundaries
iosApp/            iOS entry point
build-logic/       Convention plugins and architecture enforcement
scripts/           Project rename and feature generation helpers
```

> The template is a fullstack monorepo: `:client:*` (above) is joined to a Ktor server
> (`:server:*`) by a shared contract (`:shared:*`). See [ARCHITECTURE_SERVER.md](ARCHITECTURE_SERVER.md).

## Tech Stack

- Kotlin Multiplatform
- Compose Multiplatform
- Decompose + Essenty
- Molecule
- Metro DI
- Arrow
- kotlinx.serialization
- AndroidX DataStore
- kotlin-test, AssertK, Turbine, kotlinx.coroutines-test
- Roborazzi screenshot testing

## Running the Full Stack Locally

Everything runs on localhost with zero configuration:

```bash
scripts/dev-stack.sh              # Postgres + MinIO (docker compose) + the Ktor server on :8080
```

Then run any client's **dev variant** — every target has a prod/dev environment switch decided at
build time (Android product flavor, iOS scheme, desktop packaging flag), and the dev variant
points at the local server with the platform gotchas pre-solved (the Android emulator reaches the
host via `10.0.2.2` and the dev flavor allows cleartext HTTP; iOS simulator and desktop use
`localhost`, with the dev plist relaxing ATS for local hosts):

```bash
./gradlew -PappEnv=dev :client:desktopApp:run    # desktop
./gradlew :client:androidApp:installDevDebug     # Android emulator
# iosApp via Xcode on a simulator — scheme iosApp-dev
```

Prod variants resolve to `PROD_SERVER_BASE_URL` (a placeholder in
`client/composeApp/.../ApiConfigResolution.kt` — point it at your deployment). For physical
devices, dev builds bake the build machine's address in automatically (Android: LAN IP; iOS:
Bonjour name) — see `scripts/dev-stack.sh --lan`.

`scripts/dev-stack.sh --down` stops the containers keeping data; `--nuke` also drops the volumes
(fresh DB + bucket); `--lan` serves on your LAN IP for physical devices and makes presigned blob
URLs reachable off-machine. Details in
[ARCHITECTURE_SERVER.md § Local dev & config](ARCHITECTURE_SERVER.md).

### Running the E2E Suite

With the dev stack up and an emulator running:

```bash
scripts/e2e-android.sh            # health check → connectedDevDebugAndroidTest
```

The instrumented suite (`client/androidApp/src/androidTest`) drives the real app against the real
dev server through per-feature **robot** classes (`:client:feature:*:robots` — see
[ARCHITECTURE.md § E2E Robots](ARCHITECTURE.md)). The Test Orchestrator clears app data between
tests and each flow signs up its own account, so runs are isolated without any server-side
cleanup; failures leave a screenshot in
`client/androidApp/build/outputs/androidTest-results/`. CI compiles the suite APK on every PR
(`assembleDevDebugAndroidTest`); actually running it stays local, where a dev stack and a device
exist.

## Creating a Feature

Generate a new feature pair with:

```bash
./scripts/generate-feature.sh settings Settings
```

This creates `:client:feature:settings:public` and `:client:feature:settings:impl`, updates
`settings.gradle.kts`, runs Spotless for the new modules, and leaves the feature ready to wire into a
parent component.

Nested and core module generation are also supported:

```bash
./scripts/generate-feature.sh user/details UserDetails
./scripts/generate-feature.sh local-storage LocalStorage core
```

## Renaming the Template

```bash
./scripts/rename-project.sh --name AwesomeApp --package com.example.awesome
```

Use `--display-name` to customize the launcher name and `--dry-run` to preview changes first.

## Client-Only Variant

This full-stack tree is the single source of truth; a client-only template is derived from it
mechanically. In a fresh clone, worktree, or CI checkout:

```bash
./scripts/make-client-only.sh
```

The script removes `:server:*`, `:shared:*`, and the client pieces that only exist to exercise the
server contract, then swaps in client-only counterparts (a local fake auth repository, seam-less
networking, a two-tab main screen) from `scripts/client-only/overlay/`. The `client-only-variant`
CI job runs the script and builds the result on every PR, so the variant cannot silently rot. When
a PR changes seam-coupled client code, update `scripts/client-only/` in the same PR.

## Module Rules

The build enforces these rules:

- `:public` modules may depend only on other `:public` modules.
- `:impl` modules may depend only on `:public` modules.
- `:testing` modules may depend only on their sibling `:public` module.
- `:client:core:*` modules may not depend on `:client:feature:*` modules.
- Every `:impl` module must have a sibling `:public` module.
- Leaf modules must use the approved names: `public`, `impl`, `testing`, `composeApp`, or
  `androidApp`.

```
Copyright 2026 HarukeyUA

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```