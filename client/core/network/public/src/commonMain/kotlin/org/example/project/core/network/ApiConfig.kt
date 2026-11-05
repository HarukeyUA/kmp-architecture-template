package org.example.project.core.network

/**
 * Client → server connection config: the base URL every API request is resolved against. Resolved
 * once at graph creation from the injected environment and the platform's [ApiConfigDefaults] (see
 * `resolveApiConfig` in `:client:composeApp`) — the environment is a build/packaging-time fact,
 * never sniffed at runtime.
 *
 * [baseUrl] must be origin-only (scheme + host + port, no path segment) — dev-host injection
 * validates candidates with [normalizeOriginOrNull], and a path prefix would silently prefix every
 * shared `@Resource` route resolved against it.
 */
data class ApiConfig(val baseUrl: String)
