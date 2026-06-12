# 0003. Share the wire, never the domain

- Status: Accepted (amended 2026-06-12: server-side mapping rule made precise)
- Date: 2026-05-30

## Context

"One whole thing" tempts sharing domain models across the Seam. But the server's domain model carries DB concerns (internal ids, soft-delete flags, timestamps) and the client's carries UI concerns (display state, derived fields) — they evolve for opposite reasons.

## Decision

The Seam holds only the **Wire**: `@Resource` routes, `@Serializable` DTOs (`*Request`/`*Response`), pure shape validation, and the error taxonomy. **Each side owns its own Domain model.** The client maps Wire → Domain via `toModel()` extension functions (the convention already documented in `ARCHITECTURE.md`), with dedicated mappers only for hard transforms. DTOs that currently live in each client feature's `:public/data/models/` relocate into `:shared:<domain>`.

On the server, the route layer is the Wire boundary (amended 2026-06-12 — the original text said this less precisely and the code had drifted to wire-typed services):

- **Routes own both mapping directions.** A route unpacks `*Request` fields into plain service parameters and maps the returned Domain model to a `*Response`. Mirror "command" types that copy a request field-for-field are explicitly *not* required — unpack parameters instead; introduce a server-side input type only when a use case's input is genuinely rich.
- **Service interfaces speak Domain models, never wire DTOs.** A domain's `:public` is the only cross-domain surface (ADR-0006), so its return types are the server's internal reuse currency; wire DTOs there would leak client-compat accretion (compat-nullability) into every internal caller.
- **One deliberate exception: the error channel.** Services keep returning `Either<ApiError, T>` (ADR-0005). The error taxonomy is Seam vocabulary the server itself defines and keeps semantic — it carries no client-compat baggage, and a parallel server-domain error hierarchy would be a 1:1 mirror mapped in routes for zero benefit.
- Shape validation stays in the service (ADR-0004's "server always re-validates"); routes stay thin HTTP adapters with no logic.

## Considered options

- **Share domain models too** — rejected. Couples two models that must evolve independently; a DB change would ripple into the UI and vice versa.
- **DTOs-as-domain on the client** (the prior witchy approach) — rejected as the default. Additive-compat forces DTOs to accrue nullable fields; using them directly leaks that compat-nullability into the UI. Always mapping is also the existing client convention.

## Consequences

- A mapping step on each side — trivial via extension functions for most types.
- The wire can evolve (new fields, compat-nullability) without rippling into UI or domain logic.
- Server domain types that cross module boundaries (service returns) live in the domain's `:public`, per ADR-0006's "Service interface + cross-module domain types".
