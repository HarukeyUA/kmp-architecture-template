package org.example.project.core.network

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface AppleApiConfigGraph {
    /** The iOS simulator shares the host network, so `localhost` reaches the dev server. */
    @Provides
    @SingleIn(AppScope::class)
    fun provideApiConfig(): ApiConfig = ApiConfig(baseUrl = "http://localhost:8080")
}
