# 0003. Share the wire, never the domain

- Status: Accepted
- Date: 2026-05-30

## Context

"One whole thing" tempts sharing domain models across the Seam. But the server's domain model carries DB concerns (internal ids, soft-delete flags, timestamps) and the client's carries UI concerns (display state, derived fields) — they evolve for opposite reasons.

## Decision

The Seam holds only the **Wire**: `@Resource` routes, `@Serializable` DTOs (`*Request`/`*Response`), pure shape validation, and the error taxonomy. **Each side owns its own Domain model.** The client maps Wire → Domain via `toModel()` extension functions (the convention already documented in `ARCHITECTURE.md`), with dedicated mappers only for hard transforms; the server maps Wire ↔ its domain in the route layer. DTOs that currently live in each client feature's `:public/data/models/` relocate into `:shared:<domain>`.

## Considered options

- **Share domain models too** — rejected. Couples two models that must evolve independently; a DB change would ripple into the UI and vice versa.
- **DTOs-as-domain on the client** (the prior witchy approach) — rejected as the default. Additive-compat forces DTOs to accrue nullable fields; using them directly leaks that compat-nullability into the UI. Always mapping is also the existing client convention.

## Consequences

- A mapping step on each side — trivial via extension functions for most types.
- The wire can evolve (new fields, compat-nullability) without rippling into UI or domain logic.
