package org.example.project.server.scheduler

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.PrintWriter
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.testcontainers.containers.PostgreSQLContainer

/**
 * The Phase 6 scheduler gate (ADR-0010): a scheduled job runs **exactly once across two
 * instances**. Two [AdvisoryLockScheduler]s point at one Postgres (two app instances, one DB) and
 * contend for the same job; the Postgres advisory lock guarantees only one runs it concurrently,
 * and that the lock is released afterward so the next tick is free to reacquire.
 */
class AdvisoryLockSchedulerTest {
    @Test
    fun `only one instance runs the job when both attempt it concurrently`() {
        PostgreSQLContainer("postgres:17-alpine").use { postgres ->
            postgres.start()
            val counter = AtomicInteger(0)
            // The job parks while "running" so the second instance is guaranteed to attempt the
            // lock
            // while the first still holds it — the concurrent window the lock has to protect.
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val job =
                countingJob(counter) {
                    started.complete(Unit)
                    release.await()
                }

            hikariFor(postgres).use { dsA ->
                hikariFor(postgres).use { dsB ->
                    runBlocking {
                        val instanceA = AdvisoryLockScheduler(dsA, setOf(job))
                        val instanceB = AdvisoryLockScheduler(dsB, setOf(job))

                        val ranA = async { instanceA.runExclusively(job) }
                        started.await() // A holds the lock and is parked inside run().
                        val ranB = instanceB.runExclusively(job) // Contends now → must lose.
                        release.complete(Unit) // Let A finish and release the lock.

                        assertThat(ranA.await()).isTrue()
                        assertThat(ranB).isFalse()
                        assertThat(counter.get()).isEqualTo(1)
                    }
                }
            }
        }
    }

    @Test
    fun `the lock is released after a run so another instance can take it next tick`() {
        PostgreSQLContainer("postgres:17-alpine").use { postgres ->
            postgres.start()
            val counter = AtomicInteger(0)
            val job = countingJob(counter) {}

            hikariFor(postgres).use { dsA ->
                hikariFor(postgres).use { dsB ->
                    runBlocking {
                        // A acquires and releases; B (a different session) then acquires only
                        // because
                        // the lock was released — proving the `finally` unlock, not just exclusion.
                        assertThat(AdvisoryLockScheduler(dsA, setOf(job)).runExclusively(job))
                            .isTrue()
                        assertThat(AdvisoryLockScheduler(dsB, setOf(job)).runExclusively(job))
                            .isTrue()
                        assertThat(counter.get()).isEqualTo(2)
                    }
                }
            }
        }
    }

    @Test
    fun `start refuses two jobs that map to the same advisory-lock key`() {
        // Validation runs before any lock is taken, so no database is needed here.
        val a = countingJob(AtomicInteger()) {}
        val b = countingJob(AtomicInteger()) {} // same name → same lock key
        val scheduler = AdvisoryLockScheduler(unusedDataSource, setOf(a, b))
        val scope = CoroutineScope(Job())
        try {
            assertFailsWith<IllegalStateException> { scheduler.start(scope) }
        } finally {
            scope.cancel()
        }
    }

    /**
     * A [ScheduledJob] that counts its runs and then awaits [body] (a barrier for the lock window).
     */
    private fun countingJob(counter: AtomicInteger, body: suspend () -> Unit): ScheduledJob =
        object : ScheduledJob {
            override val name: String = "test-counting-job"
            override val interval: Duration =
                1.hours // irrelevant — the test drives runExclusively.

            override suspend fun run() {
                counter.incrementAndGet()
                body()
            }
        }

    private fun hikariFor(postgres: PostgreSQLContainer<*>): HikariDataSource =
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = postgres.jdbcUrl
                username = postgres.username
                password = postgres.password
                maximumPoolSize = 2
                driverClassName = "org.postgresql.Driver"
            }
        )

    /**
     * A [DataSource] for tests that validate before touching the DB — every method is unreachable.
     */
    private val unusedDataSource =
        object : DataSource {
            override fun getConnection() = error("unused")

            override fun getConnection(username: String?, password: String?) = error("unused")

            override fun getLogWriter() = error("unused")

            override fun setLogWriter(out: PrintWriter?) = error("unused")

            override fun setLoginTimeout(seconds: Int) = error("unused")

            override fun getLoginTimeout() = error("unused")

            override fun getParentLogger() = error("unused")

            override fun <T> unwrap(iface: Class<T>?): T = error("unused")

            override fun isWrapperFor(iface: Class<*>?) = error("unused")
        }
}
