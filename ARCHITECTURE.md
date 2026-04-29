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

**Key Rule**: Only `:composeApp` can depend on `:impl` modules. This ensures implementation details stay hidden, enables parallel compilation, and keeps coupling low.

**Layering Rule**: `:core:*` modules cannot depend on `:feature:*` modules. Core is foundation; features sit on top.

### Enforcement

The rules above are enforced at build time and are compatible with Gradle project isolation.

| Rule                                                                           | Where                                                                                                                                    |
|--------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| Module dependency rules + `:core` → `:feature`                                 | `assertModuleDependencies` task per module (runs as part of `check`). Inspects declared `project(...)` dependencies in main source sets. |
| Every leaf module is named `public`/`impl`/`testing`/`composeApp`/`androidApp` | `convention.module-structure-assert.settings` — fails at settings evaluation.                                                            |
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
├── StatefulComponent<S, E>      # Produces state, handles events
│       │                          state: StateFlow<S>, onEvent(E)
│       │
│       └── MoleculeComponent<S, E>  # Molecule-based implementation
│                                      Provides: coroutine scope, Molecule state
│                                      production, StateKeeper bridge, event channel
│
├── EventComponent<E>            # Handles events only, no state production
│                                  onEvent(E)
│
└── StackComponent<C, T>         # Manages a child stack via Decompose's childStack
                                   stack: Value<ChildStack<C, T>>, onBackClick()
```

### When to Use Each Primitive

| Primitive                 | Use When                                                 | Example                                                  |
|---------------------------|----------------------------------------------------------|----------------------------------------------------------|
| `StatefulComponent<S, E>` | Component produces reactive UI state and handles events  | `LoginComponent`, `SearchComponent`, `HomeListComponent` |
| `EventComponent<E>`       | Component handles events but delegates state to children | `MainComponent` (tab clicks, no own state)               |
| `StackComponent<C, T>`    | Component manages navigation between child components    | `RootComponent`, `HomeComponent` (list → detail)         |

These can be combined — e.g., a component could implement both `EventComponent` and `StackComponent`.

### MoleculeComponent

`MoleculeComponent<S, E>` (in `:core:component:public`) is the default implementation of `StatefulComponent`. It takes an `AppComponentContext` and delegates to it via `AppComponentContext by componentContext`. It provides:

- **Lifecycle-aware coroutine scope** — cancels on destroy
- **Molecule-powered state production** — state is produced via a `@Composable produceState()` function
- **StateKeeper integration** — bridges Essenty's StateKeeper to Compose's `SaveableStateRegistry` for process death survival
- **Event channel** — `CollectEvents {}` helper to consume UI events within the Molecule composition
- **Lifecycle bridge** — maps Essenty `Lifecycle` to AndroidX `LifecycleOwner`

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
2. Component implementation creates `override val sideEffects = SideEffects<...>()` and calls `sideEffects.send(...)` inside `CollectEvents` (or anywhere with an `AppComponentContext` in scope)
3. Screen composable calls `CollectSideEffects(component.sideEffects) { effect -> ... }` to react (e.g., animate scroll, request focus)

---

## Patterns

### Defining a StatefulComponent

In `:public`, define the interface with nested `State`, `Event`, and `Factory`:

```kotlin
interface LoginComponent : StatefulComponent<LoginComponent.State, LoginComponent.Event> {
    data class State(val counter: Int = 0) : UiState
    sealed interface Event : UiEvent { ... }
    fun interface Factory { fun create(componentContext: AppComponentContext, ...): LoginComponent }
}
```

In `:impl`, extend `MoleculeComponent` (passing `AppComponentContext`), override `produceState()`, and use `CollectEvents {}` to handle events. The Metro `@AssistedFactory` / `@ContributesBinding` annotations wire the factory into DI.

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

### Defining an EventComponent

In `:public`, define the interface with a `sealed interface Event`. For tab-based coordinators, expose a `@Serializable sealed interface Tab` as the stack configuration type so the screen can determine the active tab via `stack.active.configuration` without referencing child component types:

```kotlin
interface MainComponent : EventComponent<MainComponent.Event> {
    val snackbarHostState: SnackbarHostState
    val stack: Value<ChildStack<Tab, ScreenChild>>
    @Serializable sealed interface Tab {
        @Serializable data object Home : Tab
        @Serializable data object Search : Tab
        @Serializable data object Profile : Tab
    }
    sealed interface Event : UiEvent {
        object HomeTabClick : Event
        object SearchTabClick : Event
        object ProfileTabClick : Event
    }
}
```

In `:impl`, implement `onEvent()` to handle each event (e.g., calling `navigation.bringToFront()`). The `Tab` type doubles as Decompose's stack configuration, eliminating the need for a separate private `Config` class. The child factory pairs each feature component with its screen via `asChild`.

### Screen Interface Pattern

Screens are defined as interfaces and implemented in `:impl`. For feature entry-point screens (e.g., `HomeScreen`), the interface lives in `:public` so other features can inject and render it. For feature-internal child screens (e.g., `HomeListScreen`, `HomeDetailScreen`), both the interface and implementation live in `:impl` since they're only used within the feature's `StackComponent`.

- **StatefulComponent screens** — observe `component.state` via `collectAsStateWithLifecycle()`, delegate to a private stateless composable for previewability
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
- **MainComponent** — `EventComponent` managing bottom navigation tabs via `childStack` + `bringToFront`
- **HomeComponent** — `StackComponent` managing List → Detail navigation within the Home tab

---

## Dependency Injection

### Metro DI Patterns

**Component factories** use Metro's `@AssistedInject` / `@AssistedFactory` pattern. The factory interface is defined in `:public`, and the implementation in `:impl` is annotated with `@ContributesBinding(AppScope::class)` to auto-bind into the DI graph.

**Screen bindings** use `@Inject` + `@ContributesBinding(AppScope::class)` on the implementation class. Screens can inject other screens to compose UI hierarchies.

**App graph** is defined per-platform in `:composeApp` (`AndroidAppGraph`, `IosAppGraph`, `JvmAppGraph`), each annotated with `@DependencyGraph(AppScope::class)`. The graph exposes `rootComponentFactory` and `rootScreen` as entry points.

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
- **Components that do not extend `MoleculeComponent`** (e.g., plain `EventComponent` or `StackComponent` implementations that need to launch coroutines) — call `LifecycleOwner.lifecycleAwareScope()` from `:core:component:public`. It returns a `CoroutineScope` tied to the component's lifecycle, running on the platform main dispatcher with a `SupervisorJob`, and is cancelled automatically on destroy. Since `AppComponentContext` extends `LifecycleOwner`, this is available on any component context:
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

1. **Test via the component contract** — observe `state: StateFlow<S>` and send events via `onEvent(E)`, no UI framework needed
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

**`CoroutineTest`** — abstract base class for all component tests. Sets `Dispatchers.Main` to an `UnconfinedTestDispatcher` in `@BeforeTest` and resets it in `@AfterTest`. All test classes extend this.

**`runLifecycleTest`** — top-level function that wraps `runTest` with lifecycle management. It creates a `LifecycleRegistry`, calls `resume()` before the test body, and `destroy()` after. The lifecycle is passed into the test block so it can be forwarded to `createComponent`.

**`testComponentContext(lifecycle)`** — creates a `DefaultAppComponentContext` with `SnapshotNotifier.WhileActive`, so Molecule manages its own snapshot notifications in tests (in production, Compose UI handles this via `SnapshotNotifier.External`). All test `createComponent` helpers should use this instead of constructing `DefaultAppComponentContext` directly.

### Testing a StatefulComponent

Tests extend `CoroutineTest()` and use `runLifecycleTest` to get a managed lifecycle. A private `createComponent` helper accepts the lifecycle plus any dependencies with defaults.

**Key patterns:**
- `CoroutineTest()` — base class that sets up `UnconfinedTestDispatcher` as `Dispatchers.Main`
- `runLifecycleTest { lifecycle -> ... }` — manages `LifecycleRegistry` creation, `resume()`, and `destroy()`
- `createComponent(lifecycle, ...)` — builds `testComponentContext(lifecycle)` and the component under test
- `component.state.test { ... }` — Turbine collects the `StateFlow` and provides `awaitItem()` for assertions
- `component.onEvent(...)` — simulates UI interactions

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

```kotlin
// Example
class LoginScreenScreenshotTest : ScreenshotTest() {
    @Test
    fun loginScreen() = capture {
        LoginScreenContent(state = LoginComponent.State())
    }
}
```
