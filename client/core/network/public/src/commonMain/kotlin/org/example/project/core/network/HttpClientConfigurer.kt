package org.example.project.core.network

import io.ktor.client.HttpClientConfig

/**
 * Lets a feature add to the app's single HttpClient without :client:core:network knowing about it.
 * Contribute one into the set with `@ContributesIntoSet(AppScope::class)`.
 */
fun interface HttpClientConfigurer {
    fun configure(config: HttpClientConfig<*>)
}
