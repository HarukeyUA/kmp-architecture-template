package org.example.project.core.network

/**
 * Client → server connection config, provided per platform (and, in a real app, per build variant:
 * debug = localhost, release = prod). The platform gotchas are pre-solved in the providers: Android
 * emulator reaches the host at `10.0.2.2`, iOS simulator and desktop use `localhost`.
 */
data class ApiConfig(val baseUrl: String)
