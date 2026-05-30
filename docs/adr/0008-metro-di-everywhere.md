# 0008. Metro DI across the whole monorepo

- Status: Accepted
- Date: 2026-05-30

## Context

The server needs dependency injection. The client already uses Metro (compile-time KSP DI).

## Decision

Use **Metro on the server too**, with its own `@DependencyGraph(AppScope)` in `:server:app`. One DI mental model across client and server, and the same `@ContributesBinding` / `@ContributesIntoSet` contribution pattern on both sides (the latter aggregates routes, table sets, and error `SerializersModule`s — see ADR-0005, ADR-0006).

## Considered options

- **Koin on the server** (the Ktor-idiomatic default) — rejected. A second DI model to learn, and runtime wiring errors instead of compile-time.
- **Manual wiring in `main()`** (witchy) — rejected. Doesn't scale and provides no testability seams.

## Consequences

- Metro's ergonomics in a plain-JVM/Ktor context should be confirmed early with a small spike (low risk — KSP runs fine on the JVM).
- The contribution-based aggregation means `:server:app` never grows a hand-maintained registry.
