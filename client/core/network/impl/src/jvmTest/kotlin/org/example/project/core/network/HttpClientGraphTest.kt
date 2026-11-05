package org.example.project.core.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import io.ktor.client.HttpClient
import kotlin.test.Test
import org.example.project.core.buildinfo.Environment

class HttpClientGraphTest {
    @Test
    fun `a graph with no configurer builds the client`() {
        val graph =
            createGraphFactory<NoConfigurerGraph.Factory>()
                .create(ApiConfig("http://localhost"), Environment.PROD)

        graph.httpClient.close()

        assertThat(graph.configurers.size).isEqualTo(0)
    }

    @Test
    fun `a contributed configurer is applied to the client`() {
        var configured = false
        val graph =
            createGraphFactory<OneConfigurerGraph.Factory>()
                .create(
                    ApiConfig("http://localhost"),
                    Environment.PROD,
                    TestConfigurer(HttpClientConfigurer { configured = true }),
                )

        graph.httpClient.close()

        assertThat(configured).isTrue()
    }

    @DependencyGraph(AppScope::class)
    interface NoConfigurerGraph {
        val httpClient: HttpClient
        val configurers: Set<HttpClientConfigurer>

        @DependencyGraph.Factory
        fun interface Factory {
            fun create(
                @Provides apiConfig: ApiConfig,
                @Provides environment: Environment,
            ): NoConfigurerGraph
        }
    }

    @DependencyGraph(AppScope::class)
    interface OneConfigurerGraph {
        val httpClient: HttpClient

        @Provides
        @IntoSet
        fun provideConfigurer(configurer: TestConfigurer): HttpClientConfigurer = configurer.value

        @DependencyGraph.Factory
        fun interface Factory {
            fun create(
                @Provides apiConfig: ApiConfig,
                @Provides environment: Environment,
                @Provides configurer: TestConfigurer,
            ): OneConfigurerGraph
        }
    }

    class TestConfigurer(val value: HttpClientConfigurer)
}
