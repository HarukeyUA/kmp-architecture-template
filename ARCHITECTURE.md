# KMP + Compose Multiplatform Architecture

## Overview

Architecture of the sample project — a Kotlin Multiplatform (KMP) application using Compose Multiplatform for UI.

### Tech Stack

| Layer | Technology |
|-------|------------|
| UI | Compose Multiplatform |
| Navigation & Lifecycle | Decompose + Essenty |
| State Production | Molecule |
| Dependency Injection | Metro |
| Serialization | kotlinx.serialization |
| Local Storage | AndroidX DataStore |
| Testing | kotlin-test, AssertK, Turbine, kotlinx.coroutines.test |
| Screenshot Testing | Roborazzi |

### Design Principles

1. **Unidirectional Data Flow** — State flows down, events flow up
2. **Composition over Inheritance** — Components can contain other components
3. **Type Safety** — Kotlin's type system and serialization for navigation
4. **Platform Agnostic** — Business logic in commonMain, platform specifics isolated
5. **Testability** — Clean separation enables unit testing without UI framework
6. **Minimal Boilerplate** — Convention plugins and base classes reduce repetition

---

## Module Structure

### Public/Impl Pattern

Modules are split into `:public` and `:impl` submodules:

```
:core:component:public       # Component primitives (StatefulComponent, EventComponent,
                             #   MoleculeComponent, platform bridges)
:core:navigation:public      # Navigation primitives (StackComponent)
:core:ui:public              # UI helpers (ChildStack composable), theme, design system
:core:local-storage:public   # Local storage interface (SettingsLocalDataSource)
:core:local-storage:impl     # DataStore-based implementation
:core:testing:public         # Test utilities (CoroutineTest, runLifecycleTest)
:core:screenshot-testing:public  # Screenshot test infrastructure (ScreenshotTest via Roborazzi)

:feature:auth:public         # LoginComponent, LoginScreen interfaces
:feature:auth:impl           # Default implementations
:feature:main:public         # MainComponent, MainScreen interfaces
:feature:main:impl           # Default implementations
:feature:home:public         # HomeComponent (StackComponent), child interfaces, screens
:feature:home:impl           # Default implementations
:feature:search:public       # SearchComponent, SearchScreen interfaces
:feature:search:impl         # Default implementations
:feature:profile:public      # ProfileComponent, ProfileScreen interfaces
:feature:profile:impl        # Default implementations
:feature:user-data:public    # UserRepository interface
:feature:user-data:impl      # Default UserRepository implementation
:feature:user-data:testing   # Shared fakes (FakeUserRepository)

:composeApp                  # Wires :impl modules together, RootComponent
:androidApp                  # Android app entry point
:iosApp                      # iOS app entry point (Xcode project)
```

### Module Dependency Rules

| Module Type | Can Depend On |
|-------------|---------------|
| `:public` | Other `:public` modules only |
| `:impl` | Any `:public` module, sibling `:public` via `api` |
| `:testing` | Sibling `:public` module (exposes fakes of the public API) |
| `:composeApp` | Any module (wires `:impl` together) |

**Key Rule**: Only `:composeApp` can depend on `:impl` modules. This ensures implementation details stay hidden, enables parallel compilation, and keeps coupling low.

---

## Component Architecture

### Component Hierarchy

All components extend Decompose's `ComponentContext`, which provides lifecycle, state keeper, instance keeper, and back handler.

```
ComponentContext (Decompose)
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

| Primitive | Use When | Example |
|-----------|----------|---------|
| `StatefulComponent<S, E>` | Component produces reactive UI state and handles events | `LoginComponent`, `SearchComponent`, `HomeListComponent` |
| `EventComponent<E>` | Component handles events but delegates state to children | `MainComponent` (tab clicks, no own state) |
| `StackComponent<C, T>` | Component manages navigation between child components | `RootComponent`, `HomeComponent` (list → detail) |

These can be combined — e.g., a component could implement both `EventComponent` and `StackComponent`.

### MoleculeComponent

`MoleculeComponent<S, E>` (in `:core:component:public`) is the default implementation of `StatefulComponent`. It provides:

- **Lifecycle-aware coroutine scope** — cancels on destroy
- **Molecule-powered state production** — state is produced via a `@Composable produceState()` function
- **StateKeeper integration** — bridges Essenty's StateKeeper to Compose's `SaveableStateRegistry` for process death survival
- **Event channel** — `CollectEvents {}` helper to consume UI events within the Molecule composition
- **Lifecycle bridge** — maps Essenty `Lifecycle` to AndroidX `LifecycleOwner`

### ChildStack Composable

`ChildStack()` (in `:core:ui:public`) is a convenience wrapper around Decompose's stack rendering that takes a `StackComponent` directly and wires up back-gesture animation automatically via `backAnimation()`. Each platform provides its own `backAnimation()` implementation (predictive back on Android and iOS, fade on others).

---

## Patterns

### Defining a StatefulComponent

In `:public`, define the interface with nested `State`, `Event`, and `Factory`:

```kotlin
interface LoginComponent : StatefulComponent<LoginComponent.State, LoginComponent.Event> {
    data class State(val counter: Int = 0) : UiState
    sealed interface Event : UiEvent { ... }
    fun interface Factory { fun create(componentContext: ComponentContext, ...): LoginComponent }
}
```

In `:impl`, extend `MoleculeComponent`, override `produceState()`, and use `CollectEvents {}` to handle events. The Metro `@AssistedFactory` / `@ContributesBinding` annotations wire the factory into DI.

### Defining a StackComponent

In `:public`, define the interface with a `sealed interface Child` holding child component references:

```kotlin
interface HomeComponent : StackComponent<Any, HomeComponent.Child> {
    sealed interface Child {
        data class List(val component: HomeListComponent) : Child
        data class Detail(val component: HomeDetailComponent) : Child
    }
    fun interface Factory { fun create(componentContext: ComponentContext): HomeComponent }
}
```

In `:impl`, use Decompose's `childStack()` with `StackNavigation` and a child factory function. The `ChildStack()` composable renders the active child with back-gesture animation.

### Defining an EventComponent

In `:public`, define the interface with a `sealed interface Event`:

```kotlin
interface MainComponent : EventComponent<MainComponent.Event> {
    val stack: Value<ChildStack<*, Child>>
    sealed interface Child { ... }
    sealed interface Event : UiEvent {
        object HomeTabClick : Event
        object SearchTabClick : Event
        object ProfileTabClick : Event
    }
}
```

In `:impl`, implement `onEvent()` to handle each event (e.g., calling `navigation.bringToFront()`).

### Screen Interface Pattern

Screens are defined as interfaces in `:public` and implemented in `:impl`. This allows screens to be injected via DI, keeping `:impl` modules decoupled from each other.

- **StatefulComponent screens** — observe `component.state` via `collectAsStateWithLifecycle()`, delegate to a private stateless composable for previewability
- **StackComponent screens** — use the `ChildStack(component)` composable, matching on child types and delegating to injected child screens

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

| Plugin | Purpose |
|--------|---------|
| `kmp.library` | Base KMP library setup (targets, SDK versions) |
| `kmp.feature.public` | Public feature module (adds serialization, coroutines) |
| `kmp.feature.impl` | Impl feature module (adds Metro, auto-depends on `:public`) |
| `kmp.compose.feature.public` | Public feature with Compose (adds `:core:component:public`) |
| `kmp.compose.feature.impl` | Impl feature with Compose (adds Metro, Molecule, Decompose, `:core:component:public`, `:core:ui:public`). Also adds `commonTest` deps: kotlin-test, AssertK, Turbine, kotlinx.coroutines.test, `:core:testing:public` |
| `metro` | Metro DI setup (KSP, runtime) |
| `molecule` | Molecule setup (Compose compiler, runtime) |
| `compose` | Compose Multiplatform UI |
| `compose.resources` | Compose Multiplatform resources |
| `coroutines` | kotlinx.coroutines |
| `decompose` | Decompose setup |
| `serialization` | kotlinx.serialization |
| `screenshot.testing` | Roborazzi screenshot testing (androidHostTest sourceSet, Pixel 9 + Pixel Tablet, light/dark) |

Features that use `StackComponent` need to add `api(project(":core:navigation:public"))` to their `:public` module's dependencies.

---

## Component Platform Bridges

All component platform bridges live in `:core:component:public`:

| Bridge | Purpose |
|--------|---------|
| `EssentyLifecycleOwner` | Maps Essenty `Lifecycle` → AndroidX `LifecycleOwner` |
| `StateKeeperSaveableStateRegistry` | Maps Essenty `StateKeeper` → Compose `SaveableStateRegistry` |
| `moleculeContext()` | Platform-specific coroutine dispatcher for Molecule (expect/actual) |

---

## Testing

### Testing Stack

| Library | Purpose |
|---------|---------|
| [kotlin-test](https://kotlinlang.org/api/latest/kotlin.test/) | Test framework (`@Test`, platform-native runner) |
| [AssertK](https://github.com/willowtreeapps/assertk) | Fluent assertion library (`assertThat(x).isEqualTo(y)`) |
| [Turbine](https://github.com/cashapp/turbine) | `StateFlow` / `Flow` testing (`awaitItem()`, `expectNoEvents()`) |
| [kotlinx.coroutines.test](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/) | `runTest`, `TestScope`, virtual time control |
| [Roborazzi](https://github.com/takahirom/roborazzi) | Screenshot testing on JVM via Robolectric |

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

### Testing a StatefulComponent

Tests extend `CoroutineTest()` and use `runLifecycleTest` to get a managed lifecycle. A private `createComponent` helper accepts the lifecycle plus any dependencies with defaults.

**Key patterns:**
- `CoroutineTest()` — base class that sets up `UnconfinedTestDispatcher` as `Dispatchers.Main`
- `runLifecycleTest { lifecycle -> ... }` — manages `LifecycleRegistry` creation, `resume()`, and `destroy()`
- `createComponent(lifecycle, ...)` — builds `DefaultComponentContext(lifecycle)` and the component under test
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
