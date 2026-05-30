# 0006. Server internals mirror the client's public/impl split

- Status: Accepted
- Date: 2026-05-30

## Context

The prior witchy server was a single module with all logic inline in route handlers. It did not stay changeable: nothing stopped a route from reaching into any table, or one area from tangling into another. We want enforced boundaries and testability, consistent with the client.

## Decision

Mirror the client's `public`/`impl` split on the server.

- **Core modules (no god core):** `:server:core:{database,web,auth,observability}:public|impl`.
- **Domain slices:** `:server:feature:<domain>:public|impl` — `public` = the Service interface + cross-module domain types; `impl` = ServiceImpl, Repository, Exposed tables, routes, stateful validation, and Metro bindings.
- **`:server:app`** = thin composition root (Metro graph + `main()` + Ktor install + Flyway run + route mounting).
- The three layers **route → service → repository** live as **packages** inside each domain `:impl`, not as modules.

Disciplines:

- **The service owns the transaction** (`newSuspendedTransaction`); repositories assume an ambient transaction (fixes witchy's per-query `transaction { }` wrapping).
- **Repositories return domain types, never Exposed `ResultRow`** — Exposed is contained in the `data` package, and services become unit-testable against fake repositories.
- **Cross-domain calls are service → service only** — a domain depends on another's `:public` (its Service interface), never its tables.
- **Routes and Exposed table sets self-register** via Metro `@ContributesIntoSet`, so `:server:app` stays thin and adding a domain touches zero lines in `:app` (mirrors the client `implAggregator`).

## Considered options

- **Single `:server` module** (witchy) — rejected. Boundaries are discipline-only and rot.
- **Per-layer modules** (`:server:feature:notes:route|service|data`) — rejected. Over-engineering for a single deployable; layers as packages are enough.

## Consequences

- `public`/`impl` makes the cross-domain boundary **structural** (compiler-enforced): a domain literally cannot touch another's repository because it lives in an `:impl` it does not depend on.
- The `:testing` sibling pattern extends to the server for shared fakes.
