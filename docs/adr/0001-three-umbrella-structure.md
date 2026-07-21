# 0001. Three-umbrella module structure joined by a shared seam

- Status: Accepted
- Date: 2026-05-30

## Context

`kmp-template` is a client-only Compose Multiplatform template. We are extending it into a fullstack monorepo (client + Ktor server) and want the two halves to feel like "one whole thing" without entangling them — while keeping boundaries enforced and not foreclosing horizontal scale. The client already enforces strict `public`/`impl` layering via `module-graph-assert`.

## Decision

One Gradle build, three top-level **umbrellas**:

- `:client:*` — everything that exists today (`:core:*`, `:feature:*`) moved under `:client`.
- `:shared:*` — the **Seam**: per-domain contract modules + `:shared:common`.
- `:server:*` — the Ktor server.

The server is a **separate subsystem joined by a shared Contract**, not by interleaved code. Dependency law (the umbrella names *are* the rule):

- `:client → :shared`, `:server → :shared`, `:shared → :shared` only.
- **Never** `:client ↔ :server`.

`:shared`'s external dependency surface is rationed to `kotlinx.serialization`, `ktor-resources`, `arrow-core`, `kotlinx.datetime` — nothing else (no Compose/Decompose, no Ktor client/server engines, no Exposed, no DataStore). Enforced by extending `module-graph-assert` + `module-structure-assert`.

## Considered options

- **Feature modules that physically span client + server** — rejected. Client code is Compose/Decompose KMP; server code is JVM-only Exposed/Ktor with a different dependency set and an independent deploy cadence. Interleaving them forces ugly mixed source sets and makes the server *harder* to reason about, not easier.
- **Composite build / `includeBuild` for the server** — rejected. Reduces the "one whole thing" feeling and adds cross-build friction; the whole point is a single build where editing `:shared` recompiles both sides.
- **One fat `:shared` module** — rejected. A god module that kills incremental compilation and makes the dependency rules toothless.

## Consequences

- A one-time, mechanical migration of existing `:core:*`/`:feature:*` modules under `:client:*`.
- The Seam must be actively policed (rationed deps) to avoid rotting into a god module.
- The corrected mental model vs t3: the magic is the type-safe **Seam**, not colocated code. The payoff is structural — editing a Contract recompiles client and server in one build.
