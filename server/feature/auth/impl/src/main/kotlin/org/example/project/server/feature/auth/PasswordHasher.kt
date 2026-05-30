package org.example.project.server.feature.auth

import de.mkammerer.argon2.Argon2Factory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/** Hashes and verifies passwords. Behind an interface so the algorithm is swappable + fakeable. */
interface PasswordHasher {
    fun hash(password: String): String

    fun verify(password: String, hash: String): Boolean
}

/**
 * **Argon2id** password hashing — the baked-in, never-hand-rolled default (ADR-0009). Memory-hard
 * parameters follow current OWASP guidance (64 MiB, 3 iterations). The plaintext char array is
 * wiped after each operation so it doesn't linger on the heap.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class Argon2PasswordHasher : PasswordHasher {
    private val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)

    override fun hash(password: String): String {
        val chars = password.toCharArray()
        return try {
            argon2.hash(ITERATIONS, MEMORY_KIB, PARALLELISM, chars)
        } finally {
            argon2.wipeArray(chars)
        }
    }

    override fun verify(password: String, hash: String): Boolean {
        val chars = password.toCharArray()
        return try {
            argon2.verify(hash, chars)
        } finally {
            argon2.wipeArray(chars)
        }
    }

    private companion object {
        const val ITERATIONS = 3
        const val MEMORY_KIB = 65_536
        const val PARALLELISM = 1
    }
}
