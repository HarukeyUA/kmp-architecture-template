package org.example.project.core.network

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface JvmApiConfigGraph {
    @Provides
    @SingleIn(AppScope::class)
    fun provideApiConfig(): ApiConfig = ApiConfig(baseUrl = "http://localhost:8080")
}
