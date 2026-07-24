package org.example.project.core.network

/**
 * The build-time base URLs [ApiConfig] resolution picks between. Provided by the platform app graph
 * because the DEV host is platform knowledge (the Android emulator reaches the host machine via
 * `10.0.2.2`, everything else via `localhost`); PROD is the same deployment everywhere.
 */
data class ApiConfigDefaults(val devBaseUrl: String, val prodBaseUrl: String)
