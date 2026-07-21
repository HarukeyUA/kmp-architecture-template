# 0002. REST over Ktor Resources as the client/server contract

- Status: Accepted
- Date: 2026-05-30

## Context

We need a typed contract shared by client and server. The clients are mobile apps that **cannot be force-updated** — old versions linger in the wild for months, so the server is permanently obligated to serve clients built against older contracts.

## Decision

REST over HTTP, using **Ktor Resources** (`@Resource` typed route classes) defined in `:shared` and consumed by both `ktor-client-resources` and `ktor-server-resources` — one source of truth for routes, path params, and query params. Routes live under a `/v1` prefix from day one.

Real-time updates are deferred and, when added, will be a **separate channel (SSE/WS) layered over the REST baseline** with graceful degradation — never a dependency of core functionality.

## Considered options

- **kotlinx-rpc** — rejected. Pre-1.0 with no wire-format/backwards-compat guarantees, and no explicit versioning seam. With un-updatable mobile clients, a protocol bump or contract change risks breaking old clients with no `/v2`-style escape hatch. REST + JSON has a decades-stable additive evolution model (add nullable/defaulted fields freely; `ignoreUnknownKeys`).
- **Plain stringly-typed routes + shared DTOs** (the prior approach) — rejected. Paths and methods are duplicated on both sides and drift silently.

## Consequences

- Wire evolution is additive and nullable-by-default; a `/v2` prefix is available only if ever genuinely forced.
- The real-time enhancement layer is designed so that if it fails (old client, protocol skew, backgrounded app) the app degrades to "no live updates," never "broken."
