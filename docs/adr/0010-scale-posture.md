# 0010. Scale posture: don't foreclose horizontal scaling

- Status: Accepted
- Date: 2026-05-30

## Context

Scale matters to the project owner even though most apps built from the template won't need it. The honest reality: vertical scaling (one bigger box) goes absurdly far at this scale. The failure mode to avoid is not "too slow" — it's **baking in single-node assumptions** that silently become correctness bugs when you eventually scale out.

## Decision

Target **"don't foreclose horizontal scaling,"** not "scale now." Three rules:

1. **Per-instance state behind an interface with a tolerably-stale default.** The session cache uses a short TTL (≈30–60s) so revocation staleness is bounded, and is swappable for Redis later without touching call sites. (Rate limiting is per-node by default; global limits need a shared backing later.)
2. **The app stays stateless.** Opaque-session-via-DB (ADR-0009) + blobs to object storage via a `BlobStore` interface, **never local disk** (the most common accidental foreclosure).
3. **Background jobs are multi-node-safe from day one.** A minimal scheduled-task primitive in `:server:core` uses a Postgres advisory lock (`pg_try_advisory_lock`) so exactly one node runs each periodic job — zero extra infra — and jobs are written idempotently.

Connection pool size is configurable; remember `instances × poolSize ≤ Postgres max_connections` (PgBouncer if that ceiling is ever hit).

## Considered options

- **Premature horizontal everything** (Redis, a job queue, read replicas now) — rejected. YAGNI; vertical scaling defers all of it.
- **Single-node assumptions** (in-memory sessions, local-disk blobs, unguarded sweepers) — rejected. They silently introduce correctness bugs (double-processed jobs, lost-but-cached revocations) the moment you run a second instance.

## Consequences

- Going multi-node becomes a config change + an interface swap, never a rewrite, and never a silent correctness regression.
- Deferred until actually needed: Redis shared cache, a deferred-job queue (outbox + worker), read replicas, and OpenTelemetry tracing.
