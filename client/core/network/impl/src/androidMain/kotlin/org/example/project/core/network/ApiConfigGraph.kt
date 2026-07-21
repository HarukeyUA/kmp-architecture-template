package org.example.project.core.network

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface AndroidApiConfigGraph {
    /** The Android emulator reaches the host machine's localhost at `10.0.2.2`. */
    @Provides
    @SingleIn(AppScope::class)
    fun provideApiConfig(): ApiConfig = ApiConfig(baseUrl = "http://10.0.2.2:8080")
}
