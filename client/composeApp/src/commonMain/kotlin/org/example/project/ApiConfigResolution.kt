package org.example.project

import org.example.project.core.buildinfo.Environment
import org.example.project.core.network.ApiConfig
import org.example.project.core.network.ApiConfigDefaults
import org.example.project.core.network.normalizeOriginOrNull

// PROD is the same deployment on every platform; only the DEV host differs per platform (the
// Android emulator NATs the host's loopback behind 10.0.2.2, everything else uses plain
// localhost), which is why each platform graph supplies its own dev URL. The value below is a
// placeholder — point it at your real deployment.
internal const val PROD_SERVER_BASE_URL = "https://api.example.com"

/**
 * Builds the dev-build defaults from the build-machine address the entry point injected — Android's
 * dev-flavor `BuildConfig.DEV_SERVER_HOST` (LAN IP resolved at build time), iOS's `DevServerHost`
 * Info-dev.plist key (Bonjour name, emitted by the xcconfig generator). The host is a build-time
 * snapshot of the developer's machine, so it is validated rather than trusted: anything that
 * doesn't form a clean http origin falls back to [fallbackDevBaseUrl] (the loopback default), as
 * does an absent host (offline build, prod build, fresh clone).
 */
internal fun devApiConfigDefaults(
    injectedDevHost: String?,
    fallbackDevBaseUrl: String,
): ApiConfigDefaults {
    val injectedUrl =
        injectedDevHost?.takeIf { it.isNotBlank() }?.let { "http://${it.trim()}:8080" }
    val devBaseUrl = injectedUrl?.takeIf { normalizeOriginOrNull(it) != null } ?: fallbackDevBaseUrl
    return ApiConfigDefaults(devBaseUrl = devBaseUrl, prodBaseUrl = PROD_SERVER_BASE_URL)
}

/**
 * Resolves the [ApiConfig] the graph's `HttpClient` is built against — the single place a base URL
 * enters the app. A pure function of build-time facts: the environment is decided by the flavor /
 * scheme / packaging flag, so there is nothing to read at runtime. A downstream project that wants
 * a runtime dev-server override (a developer-settings screen) reads its persisted value here — and
 * only under [Environment.DEV], so PROD can never be redirected.
 */
internal fun resolveApiConfig(environment: Environment, defaults: ApiConfigDefaults): ApiConfig =
    ApiConfig(
        baseUrl = if (environment == Environment.PROD) defaults.prodBaseUrl else defaults.devBaseUrl
    )
