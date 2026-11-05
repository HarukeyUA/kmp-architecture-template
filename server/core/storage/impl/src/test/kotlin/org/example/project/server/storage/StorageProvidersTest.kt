package org.example.project.server.storage

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.startsWith
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import org.example.project.server.lifecycle.ServerEnvironment
import org.example.project.server.lifecycle.ServerResource
import org.example.project.server.lifecycle.closeAll

/** Only this module's contributions, so the storage DI chain is covered without a real consumer. */
@DependencyGraph(AppScope::class)
internal interface StorageTestGraph {
    val blobStore: BlobStore

    @Multibinds(allowEmpty = true) val serverResources: Set<ServerResource>

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides environment: ServerEnvironment): StorageTestGraph
    }
}

class StorageProvidersTest {
    private val completeEnv =
        mapOf(
            "S3_ENDPOINT" to "https://storage.example.com",
            "S3_REGION" to "us-east-1",
            "S3_BUCKET" to "blobs",
            "S3_ACCESS_KEY" to "key",
            "S3_SECRET_KEY" to "secret",
        )

    private fun graph(environment: ServerEnvironment): StorageTestGraph =
        createGraphFactory<StorageTestGraph.Factory>().create(environment)

    @Test
    fun `a graph without a blob consumer reads no config and closes cleanly`() = runTest {
        val graph = graph { key -> error("read '$key'") }

        graph.serverResources.closeAll()
    }

    @Test
    fun `a blob consumer resolves its config from the environment`() = runTest {
        val graph = graph { completeEnv[it] }

        assertThat(graph.blobStore.presignGet("objects/key", 5.minutes))
            .startsWith("https://storage.example.com/blobs/objects/key")
        graph.serverResources.closeAll()
    }

    @Test
    fun `a blob consumer missing any variable fails naming the key`() {
        completeEnv.keys.forEach { key ->
            val env = completeEnv - key
            val failure = assertFailsWith<IllegalStateException> { graph { env[it] }.blobStore }
            assertThat(failure.message.orEmpty()).contains(key)
        }
    }

    @Test
    fun `a blank variable fails like a missing one`() {
        val env = completeEnv + ("S3_BUCKET" to " ")
        val failure = assertFailsWith<IllegalStateException> { graph { env[it] }.blobStore }

        assertThat(failure.message.orEmpty()).contains("S3_BUCKET")
    }
}
