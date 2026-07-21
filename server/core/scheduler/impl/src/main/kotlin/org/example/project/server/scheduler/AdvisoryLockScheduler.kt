package org.example.project.server.scheduler

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.sql.Connection
import javax.sql.DataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

/**
 * Runs each [ScheduledJob] periodically, but only while holding a Postgres **advisory lock** for it
 * — so a job never runs **concurrently** on two instances, with no queue or leader election
 * (ADR-0010). The lock bounds concurrency, not frequency: each node's loop is phased off its own
 * boot, so across N nodes a job can run up to ~N times per interval — safe because jobs are
 * idempotent ([ScheduledJob] rule 1). A throwaway `pg_try_advisory_lock` is non-blocking: a node
 * that doesn't get the lock simply skips this tick rather than piling up.
 *
 * Connection accounting: a job holds one pooled connection (for the lock) for the whole time it
 * runs, plus whatever its own transactions borrow. Keep the few scheduled jobs in mind against
 * `instances × maxPoolSize ≤ Postgres max_connections` (ADR-0010) — for the handful of light jobs a
 * template has, the headroom is ample.
 */
@Inject
@SingleIn(AppScope::class)
class AdvisoryLockScheduler(
    private val dataSource: DataSource,
    private val jobs: Set<ScheduledJob>,
) {
    private val logger = LoggerFactory.getLogger(AdvisoryLockScheduler::class.java)

    /**
     * Launches one loop per job on [scope] (the Ktor application scope, so loops are cancelled on
     * shutdown). The first attempt is one interval out, so a short-lived test or a quick restart
     * never triggers a job immediately. Validates the job set first, so a misconfiguration fails
     * fast at boot rather than misbehaving in production.
     */
    fun start(scope: CoroutineScope) {
        validate()
        jobs.forEach { job ->
            scope.launch {
                while (isActive) {
                    delay(job.interval)
                    runTick(job)
                }
            }
        }
    }

    /**
     * Enforces the [ScheduledJob] invariants the type can't: a positive
     * [interval][ScheduledJob.interval], and a distinct advisory-lock key per job. The lock key is
     * a hash of the name, so this catches both duplicate names *and* the rarer hash collision the
     * doc warns about — either would silently serialize two unrelated jobs cluster-wide, so we
     * refuse to start instead.
     */
    private fun validate() {
        val byKey = HashMap<Int, ScheduledJob>()
        jobs.forEach { job ->
            require(job.interval.isPositive()) {
                "ScheduledJob '${job.name}' has a non-positive interval: ${job.interval}"
            }
            val clash = byKey.put(lockKey(job), job)
            check(clash == null) {
                "ScheduledJob lock-key collision: '${job.name}' and '${clash?.name}' map to the " +
                    "same advisory lock — rename one (job names must be unique)."
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun runTick(job: ScheduledJob) {
        try {
            runExclusively(job)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // One failed run must not kill the loop — log and try again next interval.
            logger.error("Scheduled job '{}' failed", job.name, e)
        }
    }

    /**
     * Runs [job] iff this node wins its advisory lock; returns whether it ran. The lock is held on
     * a single dedicated connection from acquisition until the job finishes, then released in a
     * `finally` so it can never leak onto a pooled connection. Visible for the multi-instance test.
     */
    suspend fun runExclusively(job: ScheduledJob): Boolean =
        withContext(Dispatchers.IO) {
            val key = lockKey(job)
            dataSource.connection.use { conn ->
                if (!tryAdvisoryLock(conn, key)) return@use false
                try {
                    job.run()
                    true
                } finally {
                    advisoryUnlock(conn, key)
                }
            }
        }

    private fun tryAdvisoryLock(conn: Connection, key: Int): Boolean =
        conn.prepareStatement("SELECT pg_try_advisory_lock(?, ?)").use { st ->
            st.setInt(1, LOCK_NAMESPACE)
            st.setInt(2, key)
            st.executeQuery().use { rs -> rs.next() && rs.getBoolean(1) }
        }

    private fun advisoryUnlock(conn: Connection, key: Int) {
        conn.prepareStatement("SELECT pg_advisory_unlock(?, ?)").use { st ->
            st.setInt(1, LOCK_NAMESPACE)
            st.setInt(2, key)
            st.execute()
        }
    }

    /** The job name's hash is the second lock key; the namespace constant scopes it to this app. */
    private fun lockKey(job: ScheduledJob): Int = job.name.hashCode()

    private companion object {
        /**
         * Fixed first key for every scheduler lock, so they can't collide with other advisory
         * locks.
         */
        const val LOCK_NAMESPACE = 0x5C_4ED // "SCHED"
    }
}
