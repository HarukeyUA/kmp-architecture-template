# KMP Architecture Template

An opinionated Kotlin Multiplatform application template for building Compose Multiplatform apps with a strongly
modular architecture, shared component logic, typed failures, and process-death-safe state.

The project targets Android, iOS, and desktop from one shared Compose codebase. The template includes navigation, lifecycle/state bridges,
DI wiring, module graph enforcement, test utilities, screenshot testing, and scripts for renaming the
project or generating new feature slices.

## TL;DR

- **Molecule + Decompose state production**: `MoleculeComponent` is the default stateful component
  primitive. Components produce `StateFlow` UI state from a `@Composable produceState()` function,
  receive UI events through an internal event channel, and live inside Decompose/Essenty component
  contexts.
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
- **Module aggregation and enforcement**: `:composeApp` automatically aggregates every `:impl`
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
androidApp/        Android entry point
iosApp/            iOS entry point
desktopApp/        JVM Desktop entry point
composeApp/        Shared app, app graph, and root navigation
core/              Reusable architecture, UI, dispatchers, networking, storage, and testing modules
feature/           Feature slices using public/impl/testing module boundaries
build-logic/       Convention plugins and architecture enforcement
scripts/           Project rename and feature generation helpers
```

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

## Creating a Feature

Generate a new feature pair with:

```bash
./scripts/generate-feature.sh settings Settings
```

This creates `:feature:settings:public` and `:feature:settings:impl`, updates
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

## Module Rules

The build enforces these rules:

- `:public` modules may depend only on other `:public` modules.
- `:impl` modules may depend only on `:public` modules.
- `:testing` modules may depend only on their sibling `:public` module.
- `:core:*` modules may not depend on `:feature:*` modules.
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