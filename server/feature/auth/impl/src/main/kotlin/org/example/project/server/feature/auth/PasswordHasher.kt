package org.example.project.server.feature.auth

import de.mkammerer.argon2.Argon2Factory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Hashes and verifies passwords. Behind an interface so the algorithm is swappable + fakeable.
 * Suspending by contract: hashing is deliberately expensive CPU work, so *where it runs* is the
 * implementation's decision — callers must never be forced to block their own dispatcher on it.
 */
interface PasswordHasher {
    suspend fun hash(password: String): String

    suspend fun verify(password: String, hash: String): Boolean

    /**
     * Burns one [verify]-equivalent of work against a throwaway credential, discarding the result.
     * For timing-equalizing failure paths that have no real hash to check (unknown email): without
     * it, response time would distinguish "no such user" (fast) from "wrong password" (one verify),
     * piercing the collapse-to-Unauthorized boundary. Lives on the interface because only the
     * implementation knows what a cost-identical dummy looks like for its algorithm and parameters.
     */
    suspend fun verifyDummy(password: String)
}

/**
 * **Argon2id** password hashing — the baked-in, never-hand-rolled default (ADR-0009). Memory-hard
 * parameters follow current OWASP guidance (64 MiB, 3 iterations). The plaintext char array is
 * wiped after each operation so it doesn't linger on the heap.
 *
 * Concurrency is bounded (ADR-0010 §11): each call costs [MEMORY_KIB] of native memory, so an
 * unbounded login flood is an OOM, not a slowdown. [hashDispatcher] caps concurrent hashes at
 * [MAX_CONCURRENT_HASHES] (≤256 MiB worst case) and moves the work off the request coroutines;
 * excess logins queue as suspended coroutines (~free) instead of being shed — the strict per-IP
 * rate limit upstream already rejects the abusive case. A code constant, not config: raising it
 * safely requires redoing the memory math, which an env var invites skipping.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class Argon2PasswordHasher : PasswordHasher {
    private val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)
    private val hashDispatcher = Dispatchers.Default.limitedParallelism(MAX_CONCURRENT_HASHES)

    /**
     * [verifyDummy]'s target, computed once at startup with the live parameters — a hardcoded hash
     * string would silently stop being cost-identical the day [ITERATIONS]/[MEMORY_KIB] change.
     */
    private val dummyHash: String =
        argon2.hash(ITERATIONS, MEMORY_KIB, PARALLELISM, "dummy".toCharArray())

    override suspend fun hash(password: String): String =
        withContext(hashDispatcher) {
            val chars = password.toCharArray()
            try {
                argon2.hash(ITERATIONS, MEMORY_KIB, PARALLELISM, chars)
            } finally {
                argon2.wipeArray(chars)
            }
        }

    override suspend fun verify(password: String, hash: String): Boolean =
        withContext(hashDispatcher) {
            val chars = password.toCharArray()
            try {
                argon2.verify(hash, chars)
            } finally {
                argon2.wipeArray(chars)
            }
        }

    override suspend fun verifyDummy(password: String) {
        // Routed through verify (and so through hashDispatcher): if the dummy skipped the
        // concurrency bound, queue depth under load would itself become the timing signal the
        // dummy exists to remove.
        verify(password, dummyHash)
    }

    private companion object {
        const val ITERATIONS = 3
        const val MEMORY_KIB = 65_536
        const val PARALLELISM = 1
        val MAX_CONCURRENT_HASHES = minOf(Runtime.getRuntime().availableProcessors(), 4)
    }
}
