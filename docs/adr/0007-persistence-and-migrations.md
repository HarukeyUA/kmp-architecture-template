# 0007. Persistence: Exposed + Postgres + Flyway, with timestamp migrations and a drift test

- Status: Accepted
- Date: 2026-05-30

## Context

The prior witchy server's migration handling felt wonky, and schema lived in one file. We want safe schema evolution that a non-backend developer can trust, consistent with the per-domain structure (ADR-0006).

## Decision

Exposed + Postgres + HikariCP + Flyway.

- **Flyway SQL is the DB source of truth; Exposed `Table` objects are the code source of truth.**
- A **Testcontainers drift test** applies all migrations to a throwaway database and **fails the build** if `MigrationUtils` reports any diff versus the Exposed schema. "Forgot a migration" becomes a red build, not a production surprise.
- Schema is **split per domain** (not one `Schema.kt`), each domain owning its tables in its `:impl`.
- Migrations are **timestamp-versioned** (`V20260530__add_notes.sql`) and **co-located** in each domain `:impl`'s resources, with Flyway scanning all locations.
- Flyway runs with `outOfOrder=false`. PR-added migrations must have a timestamp newer than the target branch's latest migration; CI enforces this with `checkMigrationOrder`.
- Flyway validates migration file names at startup so a typo is a boot failure, not a skipped migration.

## Considered options

- **Hand-written migrations only** — rejected. Drift between code and DB goes unnoticed until runtime.
- **Exposed `MigrationUtils.generateMigrationScript` as the source of truth** — rejected. Experimental and semi-manual; kept only as a drafting convenience.
- **Integer-sequence centralized migrations** — rejected. Flyway's single global integer sequence collides with per-domain co-location (duplicate `V1` across domains).

## Consequences

- Timestamp versions let each domain add migrations independently with no shared counter to coordinate.
- Ordering relies on chronological authoring (correct for cross-domain FKs in practice); the drift test and migration-order check are the safety nets.
