package org.example.project.server.scheduler

import kotlin.time.Duration

/**
 * A periodic background job, run **multi-node-safe from day one** (ADR-0010). A core or domain
 * `:impl` contributes one via `@ContributesIntoSet(AppScope::class)`; the advisory-lock scheduler
 * collects the whole `Set<ScheduledJob>` and, on every tick, runs each job only on the instance
 * that wins a Postgres advisory lock — so exactly one node runs each job at a time, with zero extra
 * infra (no queue, no leader election).
 *
 * Two rules make this safe:
 * 1. **Write jobs idempotently.** The lock prevents *concurrent* double-execution; it bounds
 *    neither how often a job runs nor where — per-node loops are independently phased, so across N
 *    nodes a job can run up to ~N times per interval. A re-run must be a no-op (e.g. "delete
 *    expired rows", not "increment a counter"). If a future job is frequency-sensitive, the
 *    upgrade path is a `scheduled_job_runs` last-run-at row checked *inside* the lock (skip when
 *    the last run is younger than the interval) — no current job needs it.
 * 2. **Keep [name] stable and unique.** It derives the advisory-lock key, so renaming a job changes
 *    which lock it takes. Uniqueness is enforced at startup — the scheduler refuses to start if two
 *    jobs would map to the same lock key — so a clash fails fast rather than silently serializing.
 */
interface ScheduledJob {
    /** Stable, unique identifier; also derives the Postgres advisory-lock key. */
    val name: String

    /** How often to attempt the job. The first attempt happens one interval after startup. */
    val interval: Duration

    /** The idempotent unit of work. Runs only on the node holding the advisory lock. */
    suspend fun run()
}
