# KMP + Compose Multiplatform Architecture

## Overview

Architecture of the sample project — a Kotlin Multiplatform (KMP) application using Compose Multiplatform for UI.

### Tech Stack

| Layer                  | Technology                                             |
|------------------------|--------------------------------------------------------|
| UI                     | Compose Multiplatform                                  |
| Navigation & Lifecycle | Decompose + Essenty                                    |
| State Production       | Molecule                                               |
| Error Handling         | Arrow (`Either`)                                       |
| Dependency Injection   | Metro                                                  |
| Serialization          | kotlinx.serialization                                  |
| Local Storage          | AndroidX DataStore                                     |
| Testing                | kotlin-test, AssertK, Turbine, kotlinx.coroutines.test |
| Screenshot Testing     | Roborazzi                                              |

### Design Principles

1. **Unidirectional Data Flow** — State flows down, events flow up
2. **Composition over Inheritance** — Components can contain other components
3. **Type Safety** — Kotlin's type system and serialization for navigation
4. **Platform Agnostic** — Business logic in commonMain, platform specifics isolated
5. **Testability** — Clean separation enables unit testing without UI framework
6. **Minimal Boilerplate** — Convention plugins and base classes reduce repetition
7. **Typed Errors over Exceptions** — Use Arrow's `Either` and `either {}` DSL; never let exceptions cross layer boundaries (see [Error Handling](#error-handling))
8. **Main-Thread-Safe Repositories** — Repository APIs are safe to call from the main thread; dispatcher selection is the implementation's responsibility (see [Concurrency and Threading](#concurrency-and-threading))
9. **Inject Dispatchers, Don't Reach for `Dispatchers.X`** — All `CoroutineDispatcher`s and the app-wide scope are obtained via DI qualifiers from `:core:dispatchers:public`
10. **Prefer Lifecycle-Scoped Coroutines** — Components launch work in their lifecycle-aware scope; the `@ApplicationCoroutineScope` is reserved for operations that *must* outlive the component

---

## Module Structure

### Public/Impl Pattern

Modules are split into `:public` and `:impl` submodules

### Module Dependency Rules

| Module Type   | Can Depend On                                              |
|---------------|------------------------------------------------------------|
| `:public`     | Other `:public` modules only                               |
| `:impl`       | Any `:public` module, sibling `:public` via `api`          |
| `:testing`    | Sibling `:public` module (exposes fakes of the public API) |
| `:composeApp` | Any module (wires `:impl` together)                        |
| `:androidApp` / `:desktopApp` | `:composeApp` (entry-point modules only)   |

**Key Rule**: Only `:composeApp` can depend on `:impl` modules. This ensures implementation details stay hidden, enables parallel compilation, and keeps coupling low. The platform entry-point modules (`:androidApp`, `:desktopApp`) depend on `:composeApp` to host the per-platform `@DependencyGraph`.

**Layering Rule**: `:core:*` modules cannot depend on `:feature:*` modules. Core is foundation; features sit on top.

### Enforcement

The rules above are enforced at build time and are compatible with Gradle project isolation.

| Rule                                                                           | Where                                                                                                                                    |
|--------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| Module dependency rules + `:core` → `:feature`                                 | `assertModuleDependencies` task per module (runs as part of `check`). Inspects declared `project(...)` dependencies in main source sets. |
| Every leaf module is named `public`/`impl`/`testing`/`composeApp`/`androidApp`/`desktopApp` | `convention.module-structure-assert.settings` — fails at settings evaluation.                                                            |
| Every `:impl` has a sibling `:public`                                          | Same settings plugin.                                                                                                                    |

Run directly with `./gradlew assertModuleDependencies` or any `check` task.

---

## Layered Architecture

Following the [Android recommended architecture](https://developer.android.com/topic/architecture), the app is organized into three layers. Data flows down from the data layer to the UI; events flow up from the UI to the data layer.

```
┌─────────────────────────────────────────────────┐
│  UI Layer (Screens, Components, UI Models)      │
├─────────────────────────────────────────────────┤
│  Domain Layer (Repositories, Domain Models,     │
│                Domain Errors)                   │
├─────────────────────────────────────────────────┤
│  Data Layer (Network DTOs, Database Entities,   │
│              API Clients)                       │
└─────────────────────────────────────────────────┘
```

### Layer Responsibilities

| Layer      | Responsibility                                                                       | Key Types                                                                                            |
|------------|--------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| **UI**     | Renders state, captures user events, maps domain models to UI models                 | Screens, Components (`MoleculeComponent`), UI Models (`*UiModel`), Error Renderers                   |
| **Domain** | Encapsulates business logic, defines repository contracts and domain-specific errors | Repository interfaces, Domain Models, Domain Errors                                                  |
| **Data**   | Implements data access, maps network/database types to domain models                 | Repository implementations, API clients, Data Sources, Network DTOs (`*Response`), mapping functions |

### Package Structure

Within each feature module, domain and presentation code is organized into separate packages:

```
:feature:example:public/
  src/commonMain/kotlin/.../feature/example/
    data/
      ExampleDataSource.kt            # Data source interface
      models/
        ExampleModel.kt               # Dto model + toModel() mapping
    domain/
      ExampleRepository.kt            # Repository interface
      models/
        ExampleModel.kt               # Domain model
        ExampleError.kt               # Domain-specific error
    presentation/
      ExampleComponent.kt             # Component interface (StatefulComponent)
      ExampleScreen.kt                # Screen interface
      models/
        ExampleUiModel.kt             # UI model + toUi() mapping

:feature:example:impl/
  src/commonMain/kotlin/.../feature/example/
    data/
      DefaultDataSource.kt            # Data source implementation
    domain/
      DefaultExampleRepository.kt     # Repository implementation
    presentation/
      DefaultExampleComponent.kt      # MoleculeComponent implementation
      ExampleErrorRenderer.kt         # Error → user message mapping
      DefaultExampleScreen.kt         # Compose UI
```

### Model Mapping Pipeline

Each layer has its own model types. Mapping functions convert between them:

```
Network DTO (data layer)         Domain Model (domain layer)       UI Model (UI layer)
───────────────────────          ──────────────────────────        ────────────────────
ExampleResponse             ──→  ExampleModel                ──→  ExampleUiModel
  @SerialName("title")            title: String                     title: String
  val title: String               ...                               ...
```

- **Response DTOs** (`*Response`) — annotated with `@Serializable` and `@SerialName` for JSON mapping. Represent the raw API contract.
- **Domain Models** — live in `feature:*:public/domain/models/`. Represent business concepts, stripped of serialization concerns.
- **UI Models** (`*UiModel`) — live in `feature:*:public/presentation/models/`. Optimized for rendering.

Mapping functions between layers:
- **DTO → Domain**: public extension functions in `:public` (e.g., `ExampleResponse.toModel()`)
- **Domain → UI**: public extension functions in `:public` (e.g., `ExampleModel.toUi()`)

### Where Each Layer Lives

| Layer      | Interfaces / Models                                          | Implementations                                            |
|------------|--------------------------------------------------------------|------------------------------------------------------------|
| **UI**     | `:public` → `presentation/` (Component, Screen, UiModel)     | `:impl` → `presentation/` (Default*, ErrorRenderer)        |
| **Domain** | `:public` → `domain/` (Repository, Models, Errors)           | `:impl` → `domain/` (DefaultRepository, mapping functions) |
| **Data**   | `:public` → `data/`  (Data source interface, *Response DTOs) | `:public` → `data/` (Data source implementation)           |

---

## Component Architecture

### Component Hierarchy

All components extend `AppComponentContext`, a custom interface that extends Decompose's `GenericComponentContext<AppComponentContext>` with app-specific capabilities (e.g., `snackbarHandler`). This means child contexts created by Decompose automatically carry app-level services.

`DefaultAppComponentContext` is the concrete implementation. It accepts a `Lifecycle` (and optional `StateKeeper`, `InstanceKeeper`, `BackHandler`, `SnackbarHandler`, `SnapshotNotifier`) and can also wrap a plain Decompose `ComponentContext`. Its `componentContextFactory` creates child contexts with `ChildSnackbarHandler` and the same `SnapshotNotifier` to form the hierarchical snackbar chain. The `snapshotNotifier` defaults to `External` (for use with Compose UI) and is read by `MoleculeComponent` when launching Molecule.

```
AppComponentContext (extends GenericComponentContext<AppComponentContext>)
│   snackbarHandler: SnackbarHandler
│
├── StatefulComponent<S>         # Produces state; user actions arrive via
│       │                          sink lambdas carried by the state
│       │                          state: StateFlow<S>
│       │
│       └── MoleculeComponent<S>     # Molecule-based implementation
│                                      Provides: coroutine scope, Molecule state
│                                      production, StateKeeper bridge
│
└── StackComponent<C, T>         # Manages a child stack via Decompose's childStack
                                   stack: Value<ChildStack<C, T>>, onBackClick()
```

User actions flow up through **event-sink lambdas carried by the state** (Circuit-style).
`StatefulComponent` has a single type parameter and exposes only `val state: StateFlow<S>`. A
coordinator that produces no data of its own is a `StatefulComponent` whose `State` carries nothing
but the sink. See [Patterns](#patterns) for the rationale.

### When to Use Each Primitive

| Primitive              | Use When                                                                        | Example                                                  |
|------------------------|---------------------------------------------------------------------------------|----------------------------------------------------------|
| `StatefulComponent<S>` | Component produces reactive UI state; user actions arrive via sink(s) on `S`     | `LoginComponent`, `SearchComponent`, `HomeListComponent` |
| `StackComponent<C, T>` | Component manages navigation between child components                            | `RootComponent`, `HomeComponent` (list → detail)         |

A stateless coordinator (e.g., a tab bar that only forwards clicks to navigation) is also a
`StatefulComponent` — its `State` is a data class carrying just the `eventSink`. These can be
combined — e.g., a component can implement both `StatefulComponent` and `StackComponent` (state +
the child stack it hosts), as `MainComponent` does.

### MoleculeComponent

`MoleculeComponent<S>` (in `:core:component:public`) is the default implementation of `StatefulComponent`. It takes an `AppComponentContext` and delegates to it via `AppComponentContext by componentContext`. It provides:

- **Lifecycle-aware coroutine scope** — cancels on destroy
- **Molecule-powered state production** — state is produced via a `@Composable produceState()` function
- **StateKeeper integration** — bridges Essenty's StateKeeper to Compose's `SaveableStateRegistry` for process death survival
- **Lifecycle bridge** — maps Essenty `Lifecycle` to AndroidX `LifecycleOwner`

`produceState()` builds the `eventSink` lambda(s) it places on the returned state; each lambda
captures the component's callbacks and `scope`, and mutates the `remember`ed Compose state (or
launches work on `scope`) when the UI invokes it.

Because the sink runs synchronously (no coroutine hop) and Molecule recomposes with
`RecompositionMode.Immediate`, a handler that mutates **two or more coupled `MutableState` holders**
must wrap them in `Snapshot.withMutableSnapshot { }` so they apply as a single emission. Without the
wrapper, atomicity is scheduling-dependent: Molecule recomposes on snapshot-apply *notifications*,
which are scheduled and coalesced rather than delivered synchronously per write — adjacent writes
usually merge into one emission (always under the `Queued` test main mode), but if a notification runs
between the writes (deterministic in tests under the `Eager` main mode; a cross-thread race in
production) the `StateFlow` publishes an intermediate, half-updated state, which can also restart a
`LaunchedEffect` keyed on one of the values mid-transition. `withMutableSnapshot` makes the atomicity
hold by construction, independent of scheduling. Writes already inside a `scope.launch { }` block need
no wrapper.

**Sink lambdas must be stable, or `StateFlow` dedup breaks.** `State` data classes compare their
`eventSink` by reference, so state equality degrades to sink identity — deduplication only works if
the Compose compiler memoizes the sink to one instance across recompositions. A sink must therefore
capture only stable identities: the component itself (for scopes/repositories/nav callbacks) and
`remember`ed snapshot-state holders (reading a delegated `var x by remember { … }` *inside* the
lambda captures the stable holder, which is fine) — never a plain value unwrapped during composition
(e.g. `val text = textFieldState.text.toString()` captured in the lambda), which changes each
recomposition, rebuilds the lambda, and makes every emission compare unequal.

### ChildStack Composable

`ChildStack()` (in `:core:ui:public`) is a convenience wrapper around Decompose's stack rendering that takes a `StackComponent` directly and wires up back-gesture animation automatically via `backAnimation()`. Each platform provides its own `backAnimation()` implementation (predictive back on Android and iOS, fade on others).

Two overloads are available:
- `ChildStack(component) { child -> ... }` — generic version where you render each child manually
- `ChildStack(component)` — convenience overload for `StackComponent<*, ScreenChild>` where each child renders itself

### Side Effects

Side effects (in `:core:component:public`) are one-time events dispatched from a component to the UI layer — things like scroll-to-top or focus a field. Unlike `UiState`, side effects are not persisted and are consumed exactly once.

| Class | Role |
|-------|------|
| `UiSideEffect` | Marker interface for side effect types (analogous to `UiEvent` / `UiState`) |
| `SideEffects<SE>` | Holds a buffered `Channel` and exposes a `Flow<SE>`. Components own an instance and call `send()` to emit |
| `CollectSideEffects` | Composable that collects the flow lifecycle-aware (`repeatOnLifecycle(STARTED)`) and invokes a callback |

**Flow:**
1. Component interface declares `val sideEffects: SideEffects<SideEffect>` and a `sealed interface SideEffect : UiSideEffect`
2. Component implementation creates `override val sideEffects = SideEffects<...>()` and calls `sideEffects.send(...)` — typically from inside an `eventSink` lambda while handling a user action, or anywhere in the Molecule composition / on `scope`
3. Screen composable calls `CollectSideEffects(component.sideEffects) { effect -> ... }` to react (e.g., animate scroll, request focus)

Side effects (one-time component→UI effects) and `StackComponent.onBackClick()` (system back) are
orthogonal to the event-sink mechanism.

---

## Patterns

### Defining a StatefulComponent

User actions are delivered **Circuit-style**: the `State` carries an `eventSink: (Event) -> Unit`
lambda that the UI invokes. The sink travels *with* the state so illegal actions can be made
unrepresentable per state.

#### Flat state — one sink for the whole screen

When every action is legal in every render, `State` is a single data class with one `eventSink`. In
`:public` (or `:impl`, for feature-internal components), define the interface with nested `State`,
`Event`, and `Factory`:

```kotlin
interface LoginComponent : StatefulComponent<LoginComponent.State> {
    data class State(val counter: Int = 0, val eventSink: (Event) -> Unit) : UiState

    sealed interface Event : UiEvent {
        data object LoginClicked : Event
    }

    fun interface Factory { fun create(componentContext: AppComponentContext, ...): LoginComponent }
}
```

In `:impl`, extend `MoleculeComponent<State>` (passing `AppComponentContext`) and override
`produceState()`. Build the sink there; it captures the component's navigation callbacks and
`remember`ed state. The Metro `@AssistedFactory` / `@ContributesBinding` annotations wire the factory
into DI:

```kotlin
@Composable
override fun produceState(): LoginComponent.State {
    var counter by rememberSaveable { mutableStateOf(0) }

    return LoginComponent.State(
        counter = counter,
        eventSink = { event ->
            when (event) {
                LoginComponent.Event.LoginClicked -> logIn()
            }
        },
    )
}
```

#### Sealed state — a sink per branch

When the screen renders as a state machine (loading spinner → content → committing), model `State`
as a `sealed interface` and put a sink **only on the branches where actions are legal**. Each
interactive branch carries its own `eventSink` typed to that branch's own event sub-interface;
blocking/terminal branches (spinners, "in progress") carry **no sink at all**, so there is literally
no way for the UI to dispatch an action that branch can't handle.

```kotlin
interface ImportComponent : StatefulComponent<ImportComponent.State> {

    sealed interface State : UiState {
        // Actions legal here → narrow sink typed to FilePickEvent.
        data class FilePick(
            val parseError: AppError? = null,
            val eventSink: (FilePickEvent) -> Unit,
        ) : State

        // No legal actions while parsing → no sink.
        data object Parsing : State

        data class Preview(
            val parsedEntryCount: Int,
            val eventSink: (PreviewEvent) -> Unit,
        ) : State

        data class Committing(val processed: Int, val total: Int) : State  // no sink
    }

    // Shared base so the event space has one supertype (SideEffect wiring, dispatch, tests).
    sealed interface Event : UiEvent

    sealed interface FilePickEvent : Event {
        data object PickFileClick : FilePickEvent
        data object BackClick : FilePickEvent      // FilePick's back exits the flow
    }

    sealed interface PreviewEvent : Event {
        data object CommitClick : PreviewEvent
        data object BackClick : PreviewEvent        // Preview's back steps back to FilePick
    }
}
```

**Why per-branch sinks and not one hoisted sink?** A single `eventSink` hoisted onto the sealed base
would have to be typed `(Event) -> Unit` — the widest type — which re-admits *every* event on
*every* branch, defeating the "illegal actions unrepresentable" goal. You also can't declare a
narrower sink on the base and override it per branch: function parameters are **contravariant**, so
an override may only *widen* the accepted parameter type, never narrow it. Making a branch accept
*fewer* events therefore requires a *separate* `eventSink` property on that branch (not an override).
The branches share nothing at the property level; only their **event types** share the base
`sealed interface Event`, which keeps the event space unified for dispatch, `SideEffect` wiring, and
tests.

**Placement rule:** put each action on the narrowest event interface where it is *always* legal. An
action legal only in `Preview` lives on `PreviewEvent`; a truly cross-cutting action legal on every
branch can live directly on the base `Event`. When two branches share an action name but its
behaviour differs (like `BackClick` above), model it per-branch. Branches with no legal action carry
no sink.

### Defining a StackComponent

In `:public`, define the interface using `ScreenChild` as the child type. This keeps child component types out of the public API:

```kotlin
interface HomeComponent : StackComponent<Any, ScreenChild> {
    fun interface Factory { fun create(componentContext: AppComponentContext): HomeComponent }
}
```

`ScreenChild` (in `:core:ui:public`) is a self-rendering child — it pairs a component with its screen so the host screen doesn't need to know about individual child types. The `asChild` extension function creates one by binding a component to its screen:

```kotlin
fun <T> T.asChild(screen: @Composable (T) -> Unit): ScreenChild
```

In `:impl`, use Decompose's `childStack()` with `StackNavigation`. The child factory creates components and pairs them with their screens via `asChild`. Child component and screen interfaces live in `:impl` since they're internal to the feature:

```kotlin
// In DefaultHomeComponent (impl)
private fun createChild(config: Config, componentContext: AppComponentContext): ScreenChild =
    when (config) {
        Config.List ->
            homeListComponentFactory.create(componentContext, ::onItemSelected)
                .asChild(homeListScreen::Content)
        is Config.Detail ->
            homeDetailComponentFactory.create(componentContext, config.itemId, navigation::pop)
                .asChild(homeDetailScreen::Content)
    }
```

The host screen uses the `ChildStack()` convenience overload — each child renders itself, so no child-type matching is needed:

```kotlin
// In DefaultHomeScreen (impl)
@Composable
override fun Content(component: HomeComponent) {
    ChildStack(component)
}
```

### Defining a stateless coordinator

A coordinator that produces no data of its own (e.g., a tab bar that only forwards clicks to
navigation) is a `StatefulComponent` whose `State` carries **only** the `eventSink`. When the
coordinator also hosts a child stack, it implements `StackComponent` too. For tab-based
coordinators, expose a `@Serializable sealed interface Tab` as the stack configuration type so the
screen can determine the active tab via `stack.active.configuration` without referencing child
component types:

```kotlin
interface MainComponent : StatefulComponent<MainComponent.State>, StackComponent<MainComponent.Tab, ScreenChild> {

    val snackbarHostState: SnackbarHostState

    data class State(val eventSink: (Event) -> Unit) : UiState

    @Serializable sealed interface Tab {
        @Serializable data object Home : Tab
        @Serializable data object Search : Tab
        @Serializable data object Profile : Tab
    }
    sealed interface Event : UiEvent {
        data object HomeTabClick : Event
        data object SearchTabClick : Event
        data object ProfileTabClick : Event
    }
}
```

In `:impl`, `produceState()` returns a `State` whose `eventSink` dispatches each event (e.g., calling
`navigation.bringToFront(Tab.Home)`). The `Tab` type doubles as Decompose's stack configuration,
eliminating the need for a separate private `Config` class. The child factory pairs each feature
component with its screen via `asChild`.

### Screen Interface Pattern

Screens are defined as interfaces and implemented in `:impl`. For feature entry-point screens (e.g., `HomeScreen`), the interface lives in `:public` so other features can inject and render it. For feature-internal child screens (e.g., `HomeListScreen`, `HomeDetailScreen`), both the interface and implementation live in `:impl` since they're only used within the feature's `StackComponent`.

- **StatefulComponent screens** — observe `component.state` via `collectAsStateWithLifecycle()`, then dispatch user actions by invoking the sink on the current state (`state.eventSink(Event.BackClick)`). For sealed state, `when`-branch on the state and reach the branch's own sink (`state.eventSink(FilePickEvent.PickFileClick)`). Delegate to a private stateless content composable that takes the sink-bearing `State` (plus separately hoisted `onX: () -> Unit` lambdas where applicable) — previews supply the state with an inert `eventSink = {}` stub
- **StackComponent screens** — use the `ChildStack(component)` convenience overload; each `ScreenChild` renders itself, so the host screen has no child-specific knowledge

---

## Navigation

### Navigation Model

All navigation uses Decompose's `childStack` with `@Serializable` configuration classes. The stack is observable via `Value<ChildStack<C, T>>`.

### App Flow

```
┌─────────────┐
│   Splash    │
└──────┬──────┘
       │ (async auth check)
       ▼
┌─────────────┐
│    Login    │
└──────┬──────┘
       │ (login success)
       ▼
┌──────────────────────────────────────────┐
│                  Main                    │
│  ┌───────────┬───────────┬────────────┐  │
│  │   Home    │  Search   │  Profile   │  │
│  │  (tab)    │  (tab)    │  (tab)     │  │
│  └─────┬─────┴───────────┴────────────┘  │
│        │                                 │
│   ┌────┴─────┐                           │
│   │ List     │ ← HomeComponent           │
│   │  ↕       │   (StackComponent)        │
│   │ Detail   │                           │
│   └──────────┘                           │
└──────────────────────────────────────────┘
       │ (logout)
       ▼
┌─────────────┐
│    Login    │
└─────────────┘
```

- **RootComponent** — `StackComponent` managing Splash → Login → Main flow; start destination is determined by `UserRepository.isLoggedIn`
- **MainComponent** — stateless coordinator (`StatefulComponent` + `StackComponent`) managing bottom navigation tabs via `childStack` + `bringToFront`
- **HomeComponent** — `StackComponent` managing List → Detail navigation within the Home tab

---

## Dependency Injection

### Metro DI Patterns

**Component factories** use Metro's `@AssistedInject` / `@AssistedFactory` pattern. The factory interface is defined in `:public`, and the implementation in `:impl` is annotated with `@ContributesBinding(AppScope::class)` to auto-bind into the DI graph.

**Screen bindings** use `@Inject` + `@ContributesBinding(AppScope::class)` on the implementation class. Screens can inject other screens to compose UI hierarchies.

**App graph** is defined per-platform: `AndroidAppGraph` in `:composeApp` (`androidMain`), `IosAppGraph` in `:composeApp` (`iosMain`), and `JvmAppGraph` in `:desktopApp`. Each is annotated with `@DependencyGraph(AppScope::class)` and exposes `rootComponentFactory` and `rootScreen` as entry points.

---

## Convention Plugins

| Plugin                       | Purpose                                                                                                                                                                                                                                  |
|------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `kmp.library`                | Base KMP library setup (targets, SDK versions)                                                                                                                                                                                           |
| `kmp.feature.public`         | Public feature module (adds serialization, coroutines)                                                                                                                                                                                   |
| `kmp.feature.impl`           | Impl feature module (adds Metro, Arrow, auto-depends on `:public`)                                                                                                                                                                       |
| `kmp.compose.feature.public` | Public feature with Compose (adds `:core:component:public`, compose resources)                                                                                                                                                           |
| `kmp.compose.feature.impl`   | Impl feature with Compose (adds Metro, Molecule, Decompose, compose resources, `:core:component:public`, `:core:ui:public`). Also adds `commonTest` deps: kotlin-test, AssertK, Turbine, kotlinx.coroutines.test, `:core:testing:public` |
| `metro`                      | Metro DI setup (KSP, runtime)                                                                                                                                                                                                            |
| `molecule`                   | Molecule setup (Compose compiler, runtime)                                                                                                                                                                                               |
| `compose`                    | Compose Multiplatform UI                                                                                                                                                                                                                 |
| `compose.resources`          | Compose Multiplatform resources (with `packageOfResClass` auto-configuration)                                                                                                                                                            |
| `arrow`                      | Arrow functional programming (arrow-core, arrow-fx-coroutines)                                                                                                                                                                           |
| `coroutines`                 | kotlinx.coroutines                                                                                                                                                                                                                       |
| `decompose`                  | Decompose setup                                                                                                                                                                                                                          |
| `serialization`              | kotlinx.serialization                                                                                                                                                                                                                    |
| `screenshot.testing`         | Roborazzi screenshot testing (androidHostTest sourceSet, Pixel 9 + Pixel Tablet, light/dark)                                                                                                                                             |

Features that use `StackComponent` need to add `api(project(":core:ui:public"))` to their `:public` module's dependencies (this transitively provides `:core:navigation:public`).

---

## Snackbar System

The snackbar system (in `:core:component:public`) enables any component to display snackbar messages that bubble up through the component hierarchy to the nearest host.

### Architecture

| Class                                   | Role                                                                                                          |
|-----------------------------------------|---------------------------------------------------------------------------------------------------------------|
| `SnackbarHandler`                       | Interface with `showSnackbar(message)`, `registerHost(callback)`, `unregisterHost(callback)`                  |
| `SnackbarDispatcher`                    | Root implementation — forwards messages to the registered host (or drops them)                                |
| `ChildSnackbarHandler`                  | Child implementation — forwards to local host if registered, otherwise bubbles up to parent                   |
| `SnackbarHostState`                     | `SnackbarHostCallback` that exposes received messages as a `SharedFlow`                                       |
| `SnackbarMessage`                       | Data class with `text` and `duration`                                                                         |
| `snackbarHost()`                        | `AppComponentContext` extension that creates and registers a `SnackbarHostState`, auto-unregisters on destroy |
| `rememberDispatchedSnackbarHostState()` | Composable that bridges `SnackbarHostState` → Compose `SnackbarHostState`                                     |

### Flow

1. `DefaultAppComponentContext` is created with a `SnackbarDispatcher` at the root
2. Child contexts automatically get a `ChildSnackbarHandler(parent = ...)` via `componentContextFactory`
3. A host component (e.g., `MainComponent`) calls `snackbarHost()` to register as the display point
4. Any descendant component calls `snackbarHandler.showSnackbar(SnackbarMessage(...))` — the message bubbles up to the nearest registered host

---

## Error Handling

The app uses typed, functional error handling with `Either<AppError, T>` (Arrow) instead of exceptions. Errors are defined per layer, wrapped as they cross layer boundaries, and rendered into localized user-facing messages via a composable renderer system.

### Rules

1. **Typed errors over exceptions.** Any function that can fail in a recoverable way returns `Either<E, T>` where `E : AppError`. Do not throw, and do not let an exception escape across a `:public` API boundary. Throwing is reserved for genuinely unrecoverable programmer errors (e.g., `IllegalStateException` for broken invariants).
2. **Compose with the `either { }` DSL.** Build multi-step error flows with the `arrow.core.raise.either { }` DSL and `.bind()`. Avoid manual `flatMap` chains and avoid `try`/`catch` in feature/domain code.
3. **Wrap external exception-throwing APIs with `catch`.** Any third-party or platform API that signals failures via exceptions (Ktor calls, `kotlinx.serialization`, `DataStore`, JNI, platform SDKs, etc.) must be converted into a typed error at the boundary using Arrow's `arrow.core.raise.catch`. The exception must not propagate further. See `core:network:public/SafeRequest.kt` for the canonical pattern:
   ```kotlin
   either {
       val response = catch({ block() }) { e -> raise(NetworkError.Connection(e)) }
       catch({ transform(response) }) { e -> raise(NetworkError.Serialization(e)) }
   }
   ```
4. **`runCatching` is not a substitute.** It silently captures `CancellationException` and produces an untyped `Throwable`. Use Arrow's `catch` so cancellation propagates correctly and the error type is explicit.
5. **No exception-based control flow across module boundaries.** If a `:public` API needs to expose a failure, model it as part of the return type. `AppErrorException` exists only for adapting to third-party APIs that *require* a `Throwable` (e.g., Paging3's `LoadResult.Error`); it is not for inter-module signaling.

### Error Type Hierarchy

All errors implement `AppError` (marker interface in `core:error:public`):

```
AppError (marker interface)
├── NetworkError (sealed interface, core:error:public)
│   ├── Http(code: Int, message: String?)
│   ├── Connection(cause: Throwable)
│   └── Serialization(cause: Throwable)
│
└── FeatureError (sealed interface, feature:*:public)
    ├── SomeSpecificError(...)
    └── Network(delegate: NetworkError) ← implements DelegatingError
```

- **`AppError`** — marker interface for all typed errors
- **`DelegatingError`** — interface for feature errors that wrap a lower-level error (exposes `delegate: AppError`)
- **`AppErrorException`** — wraps `AppError` as a `Throwable` for APIs that require exceptions (e.g., Paging3's `LoadResult.Error`)

### Error Flow Across Layers

```
Data Layer                    Domain Layer                   UI Layer
──────────                    ────────────                   ────────
API returns                   Repository maps to             Component stores in state,
Either<NetworkError, T>  ──→  Either<FeatureError, T>  ──→   Screen renders via ErrorRenderer
                              (wraps NetworkError in
                              FeatureError.Network)
```

**Data → Domain**: Repositories use Arrow's `either` DSL to map `NetworkError` into domain-specific errors:

```kotlin
override suspend fun getData(id: String): Either<FeatureError, Model> =
    either {
        api.getData(id)
            .mapLeft { FeatureError.Network(it) }  // Wrap NetworkError
            .map { it.toModel() }
            .bind()
    }
```

**Domain → UI**: Components store `AppError` in state. Screens render it via the error renderer:

```kotlin
// In component (produceState)
repository.getData(id).fold(
    ifLeft = { error -> state.error = error },
    ifRight = { data -> state.data = data },
)

// In screen (composable)
val errorText = state.error?.message()  // Resolves via LocalErrorRenderer
```

### Error Rendering System

The rendering system (in `core:ui:public`) translates `AppError` instances into localized strings:

| Class                         | Role                                                                                                                              |
|-------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| `ErrorRenderer<T : AppError>` | Interface — resolves an error to a `StringResource` (with optional format args), or renders it as a `String`                      |
| `CompositeErrorRenderer`      | DI-injected root renderer — delegates to registered renderers, handles `DelegatingError` recursion, falls back to generic message |
| `ErrorRendererGraph`          | Metro `@Multibinds` interface collecting all renderer contributions                                                               |

### Adding Error Handling to a New Feature

1. Define a sealed error type in `:public` implementing `AppError`. Wrap cross-layer errors with `DelegatingError`:

```kotlin
sealed interface MyFeatureError : AppError {
    data class NotFound(val id: String) : MyFeatureError
    data class Network(override val delegate: NetworkError) : MyFeatureError, DelegatingError
}
```

2. Return `Either<MyFeatureError, T>` from repository methods

3. Create an `ErrorRenderer<MyFeatureError>` in `:impl` with `@ContributesIntoSet`. Return `null` for delegating variants — `CompositeErrorRenderer` handles them automatically:

```kotlin
@Inject
@ContributesIntoSet(AppScope::class)
class MyFeatureErrorRenderer : ErrorRenderer<MyFeatureError> {
    override fun resolveResource(error: MyFeatureError): ResourceResult? = when (error) {
        is MyFeatureError.NotFound -> ResourceResult(Res.string.error_not_found, arrayOf(error.id))
        is MyFeatureError.Network -> null  // Delegate to NetworkErrorRenderer
    }
}
```

4. Add localized strings in `composeResources/values/strings.xml`

---

## Concurrency and Threading

All threading concerns — dispatcher selection, scope ownership, and main-thread safety — are governed by the rules below. The `:core:dispatchers:public` module owns the qualifiers used throughout the app.

### Injected Dispatchers

Direct references to `Dispatchers.IO`, `Dispatchers.Default`, or `Dispatchers.Main` are **not** allowed in feature or repository code. Dispatchers are injected via Metro qualifiers so they can be replaced with `TestDispatcher`s in tests:

| Qualifier                    | Purpose                                                              |
|------------------------------|----------------------------------------------------------------------|
| `@MainDispatcher`            | UI-thread work (typically the platform's main dispatcher)            |
| `@IoDispatcher`              | Blocking I/O (network, disk, DataStore, JNI)                         |
| `@DefaultDispatcher`         | CPU-bound work (parsing, hashing, image decoding)                    |
| `@ApplicationCoroutineScope` | App-wide `CoroutineScope` (`SupervisorJob`) for fire-and-forget work |

```kotlin
@Inject
@ContributesBinding(AppScope::class)
class DefaultExampleRepository(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val api: ExampleApi,
) : ExampleRepository { ... }
```

The only places that may reference `Dispatchers.X` directly are:
- `:core:dispatchers:impl` — the binding module that *provides* them
- Platform-specific `expect/actual` dispatcher bridges in `:core:component:public` (e.g., `mainCoroutineContext()`)
- Test infrastructure in `:core:testing:public`

### Main-Thread-Safe Repositories

Every repository method exposed by a `:public` interface **must be safe to call from the main thread.** The implementation is responsible for switching to the appropriate dispatcher (typically `@IoDispatcher`) using `withContext` *inside* the method body. Callers — components, other repositories, screens — are not expected to wrap repository calls in `withContext` themselves.

```kotlin
override suspend fun getData(id: String): Either<FeatureError, Model> =
    withContext(ioDispatcher) {
        either {
            api.getData(id)
                .mapLeft { FeatureError.Network(it) }
                .map { it.toModel() }
                .bind()
        }
    }
```

For `Flow`-returning APIs, apply `.flowOn(ioDispatcher)` at the boundary so collection on the main thread does not block it.

### Coroutine Scope Selection

Components have a built-in lifecycle-aware coroutine scope that is cancelled when the component is destroyed. **This is the default scope for all component-initiated work.**

- **`MoleculeComponent` subclasses** — already expose a lifecycle-aware `CoroutineScope`; use it directly.
- **Components that do not extend `MoleculeComponent`** (e.g., plain `StackComponent` implementations that need to launch coroutines) — call `LifecycleOwner.lifecycleAwareScope()` from `:core:component:public`. It returns a `CoroutineScope` tied to the component's lifecycle, running on the platform main dispatcher with a `SupervisorJob`, and is cancelled automatically on destroy. Since `AppComponentContext` extends `LifecycleOwner`, this is available on any component context:
  ```kotlin
  class DefaultMyComponent(
      componentContext: AppComponentContext,
  ) : MyComponent, AppComponentContext by componentContext {
      private val scope = lifecycleAwareScope()
      // launch lifecycle-bound work in `scope`
  }
  ```

The `@ApplicationCoroutineScope` is reserved for operations that *must* outlive the originating component — e.g., a logout request that needs to complete after the screen is gone, or telemetry emission tied to app lifetime. Reaching for it as a convenience to escape structured concurrency is a code smell; prefer the lifecycle-aware scope and let cancellation propagate.

| Need                                                                                                        | Use                                                                                          |
|-------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------|
| State production, event handling, data loads bound to a screen                                              | `MoleculeComponent`'s built-in scope, or `lifecycleAwareScope()` for non-Molecule components |
| One-shot work that must survive component destruction (logout, analytics flush, write-through cache update) | `@ApplicationCoroutineScope`                                                                 |

When `@ApplicationCoroutineScope` is used, it should be a deliberate decision documented in code (a brief comment explaining *why* it must outlive the component is appropriate).

---

## Component Platform Bridges

All component platform bridges live in `:core:component:public`:

| Bridge                             | Purpose                                                             |
|------------------------------------|---------------------------------------------------------------------|
| `EssentyLifecycleOwner`            | Maps Essenty `Lifecycle` → AndroidX `LifecycleOwner`                |
| `StateKeeperSaveableStateRegistry` | Maps Essenty `StateKeeper` → Compose `SaveableStateRegistry`        |
| `mainCoroutineContext()`           | Platform-specific main coroutine context (expect/actual)            |

---

## Testing

### Testing Stack

| Library                                                                                           | Purpose                                                          |
|---------------------------------------------------------------------------------------------------|------------------------------------------------------------------|
| [kotlin-test](https://kotlinlang.org/api/latest/kotlin.test/)                                     | Test framework (`@Test`, platform-native runner)                 |
| [AssertK](https://github.com/willowtreeapps/assertk)                                              | Fluent assertion library (`assertThat(x).isEqualTo(y)`)          |
| [Turbine](https://github.com/cashapp/turbine)                                                     | `StateFlow` / `Flow` testing (`awaitItem()`, `expectNoEvents()`) |
| [kotlinx.coroutines.test](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/) | `runTest`, `TestScope`, virtual time control                     |
| [Roborazzi](https://github.com/takahirom/roborazzi)                                               | Screenshot testing on JVM via Robolectric                        |

### Testing Principles

1. **Test via the component contract** — observe `state: StateFlow<S>` and drive the component by reaching the `eventSink` on the current state (`state.value.eventSink(...)`), no UI framework needed. For sealed state, narrow to a branch with the `asBranch<Branch>()` helper first: `state.value.asBranch<State.FilePick>().eventSink(FilePickEvent.PickFileClick)`
2. **Fakes over mocks** — hand-written fake implementations, not mocking libraries
3. **Shared fakes in `:testing` modules** — reusable across feature tests
4. **Tests live in `commonTest`** — in `:impl` modules, next to the implementation

### Test Organization

```
:feature:auth:impl/
  src/commonMain/kotlin/          # Production code
  src/commonTest/kotlin/          # Unit tests (DefaultLoginComponentTest)
  src/androidHostTest/kotlin/     # Screenshot tests (LoginScreenScreenshotTest)

:feature:user-data:testing/       # Shared fakes (FakeUserRepository)
  src/commonMain/kotlin/
```

The `kmp.compose.feature.impl` convention plugin auto-provides all core test dependencies (`kotlin-test`, `AssertK`, `Turbine`, `kotlinx.coroutines.test`, `:core:testing:public`). Feature modules only need to add feature-specific fakes:

```kotlin
// :feature:auth:impl build.gradle.kts — only the :testing dep is manual
commonTest.dependencies { implementation(project(":feature:user-data:testing")) }
```

### Creating Fakes

Fakes implement the `:public` interface with controllable state. Shared fakes live in a `:testing` module so multiple features can reuse them.

- Constructor parameters set initial state (e.g., `FakeUserRepository(isLoggedIn = true)`)
- `MutableStateFlow` backing fields allow tests to observe state changes via Turbine
- Spy fields (e.g., `setIsLoggedInCalled`, `logoutCalled`) track method invocations without a mocking library

See `FakeUserRepository` in `:feature:user-data:testing` for the reference implementation.

### Core Testing Utilities (`:core:testing:public`)

**`runLifecycleTest`** — top-level function that wraps `runTest` with lifecycle management and `Dispatchers.Main` setup. It creates a `LifecycleRegistry`, calls `resume()` before the test body, and reliably calls `destroy()` plus `resetMain()` afterwards (in a `finally` block, so they run even when the body times out or fails). The lifecycle is passed into the test block so it can be forwarded to `createComponent`.

The `mainMode` parameter selects how coroutines launched on `Dispatchers.Main` are driven during the test:

| Mode                                       | Dispatcher                 | When to use                                                                                                                                                                                   |
|--------------------------------------------|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `LifecycleTestMainMode.Queued` *(default)* | `StandardTestDispatcher`   | Work runs only as the scheduler advances. Pair with Turbine's `awaitItem()` (or explicit `runCurrent()` / `advanceUntilIdle()`) to observe each intermediate state emission.                  |
| `LifecycleTestMainMode.Eager`              | `UnconfinedTestDispatcher` | Launched coroutines start eagerly on the calling thread. Useful when asserting on a side effect immediately after dispatching an event (e.g. checking a navigation callback right after invoking an event sink). |

**`runCoroutineTest`** — same dispatcher / mode handling as `runLifecycleTest`, without lifecycle management. Use for tests that don't involve a component lifecycle.

**`testComponentContext(lifecycle)`** — creates a `DefaultAppComponentContext` with `SnapshotNotifier.WhileActive`, so Molecule manages its own snapshot notifications in tests (in production, Compose UI handles this via `SnapshotNotifier.External`). All test `createComponent` helpers should use this instead of constructing `DefaultAppComponentContext` directly.

**`asBranch<Branch>()`** — narrows a sealed state (or a nested branch holder) to `Branch`, throwing a clear `AssertionError` if it is some other branch (or `null`). Use it to reach a branch's `eventSink` before dispatching: `component.state.value.asBranch<State.FilePick>().eventSink(FilePickEvent.PickFileClick)`.

### Testing a StatefulComponent

Tests use `runLifecycleTest` to get a managed lifecycle and a test-scheduler-backed `Dispatchers.Main`. A private `createComponent` helper accepts the lifecycle plus any dependencies with defaults.

**Key patterns:**
- `runLifecycleTest { lifecycle -> ... }` — manages `LifecycleRegistry` creation, `resume()`, `destroy()`, and `Dispatchers.Main`
- `createComponent(lifecycle, ...)` — builds `testComponentContext(lifecycle)` and the component under test
- `component.state.test { ... }` — Turbine collects the `StateFlow` and provides `awaitItem()` for assertions
- `state.value.eventSink(...)` (or `awaitItem().eventSink(...)` inside a Turbine block) — simulates UI interactions by invoking the sink on the current state. For sealed state, narrow first with `state.value.asBranch<Branch>().eventSink(...)`

See `DefaultLoginComponentTest` in `:feature:auth:impl` for a full example.

### Testing a Component with Navigation Callbacks

Components that receive navigation callbacks (e.g., `onLoginSuccess`, `onItemSelected`) are tested by capturing the callback invocation. The callback writes to a local variable or `MutableStateFlow`, and the test asserts on it after the event.

See `DefaultHomeListComponentTest` and `DefaultHomeDetailComponentTest` in `:feature:home:impl` for examples.

### Screenshot Testing (`:core:screenshot-testing:public`)

**`ScreenshotTest`** — abstract base class for screenshot tests. Uses Roborazzi to capture composable snapshots on the JVM (via Robolectric), without a physical device.

- Extends this class and call `capture(name) { /* composable */ }` inside a `@Test`
- Each capture runs in both **light and dark** theme
- Captures are taken at two device profiles: **Pixel 9** and **Pixel Tablet**
- Screenshots are saved to `src/androidHostTest/screenshots/` and committed to version control for diff review

Feature modules add screenshot tests in `src/androidHostTest/kotlin/` and apply the `screenshot.testing` convention plugin.

Screenshot tests target the private stateless content composable, constructing the sink-bearing
`State` with an inert `eventSink = {}` stub (and stubbing any separately hoisted `onX` lambdas):

```kotlin
// Example
class LoginScreenScreenshotTest : ScreenshotTest() {
    @Test
    fun loginScreen() = capture {
        LoginScreenContent(state = LoginComponent.State(eventSink = {}))
    }
}
```
