# 0004. Validation split: pure shape shared, stateful server-only, server always re-validates

- Status: Accepted
- Date: 2026-05-30

## Context

Sharing validation across the Seam is attractive for instant client feedback, but not all validation can be shared safely — some checks need server state or the request identity.

## Decision

Split validation by the rule **"does this check need state or identity?"**:

- **Shared** (in the Seam): pure, total, context-free **shape** checks — required fields, length/format/range, enum membership, size caps — expressed as smart constructors returning `Either<ValidationError, T>`. The client runs these for instant feedback; the server runs the **same code**.
- **Server-only**: stateful/identity checks — uniqueness, existence, authorization, quota — which require the DB or the Principal.

The server **always re-validates shape**, even though the client already did.

## Considered options

- **Trust client validation** — rejected. Client validation is a UX optimization, never a security boundary.
- **All validation server-only** — rejected. Loses instant client feedback and the shared-code unity.

## Consequences

- The shape-validation duplication (client + server) is the point, not waste — it *is* the security boundary.
- Pure shared validation has zero coupling: the instant a check needs state, it is server-only by definition, so the Seam stays clean automatically.
