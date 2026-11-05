package org.example.project.server

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import kotlin.test.Test
import org.example.project.server.testing.serverGraphTest

/**
 * The background jobs every domain contributed, asserted on the assembled graph: a job that stops
 * being registered stops running, and nothing in the HTTP surface would notice.
 */
class ScheduledJobsTest {
    @Test
    fun `every domain's sweeper is registered exactly once`() = serverGraphTest { _, graph ->
        assertThat(graph.scheduledJobs.map { it.name })
            .containsExactlyInAnyOrder("auth.expired-session-sweep")
    }
}
