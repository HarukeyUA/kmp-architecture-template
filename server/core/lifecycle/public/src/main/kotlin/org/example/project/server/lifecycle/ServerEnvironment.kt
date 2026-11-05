package org.example.project.server.lifecycle

/**
 * Where the server reads its configuration. A graph input rather than ambient state, so config that
 * resolves on first use (long after boot) can still be pointed at a test's values.
 */
fun interface ServerEnvironment {
    /** The raw value of [key], or null when it is unset. */
    operator fun get(key: String): String?

    companion object {
        /** The process environment; the only place the server reads `System.getenv`. */
        val Process: ServerEnvironment = ServerEnvironment(System::getenv)
    }
}

/** The value of [key]; blank counts as missing, and missing fails naming the key. */
fun ServerEnvironment.required(key: String): String =
    this[key]?.takeIf { it.isNotBlank() } ?: error("Missing required configuration '$key'")
