package org.example.project.server.observability

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import org.slf4j.LoggerFactory

class CallLoggingPluginInstallerTest {
    @Test
    fun `debug logging includes safe request diagnostics and correlation id`() {
        val event = loggedRequest(Level.DEBUG)

        assertThat(event.formattedMessage).contains("201 Created: POST - /widgets in ")
        assertThat(event.formattedMessage).contains("[remote=")
        assertThat(event.formattedMessage).contains("contentType=application/json")
        assertThat(event.formattedMessage).contains("contentLength=2")
        assertThat(event.formattedMessage).doesNotContain("secret")
        assertThat(event.formattedMessage).doesNotContain("sensitive")
        assertThat(event.mdcPropertyMap[Correlation.MDC_KEY]).isEqualTo(REQUEST_ID)
    }

    @Test
    fun `info logging keeps the compact production request line`() {
        val event = loggedRequest(Level.INFO)

        assertThat(event.formattedMessage).contains("201 Created: POST - /widgets in ")
        assertThat(event.formattedMessage).doesNotContain("[remote=")
        assertThat(event.formattedMessage).doesNotContain("secret")
        assertThat(event.formattedMessage).doesNotContain("sensitive")
        assertThat(event.mdcPropertyMap[Correlation.MDC_KEY]).isEqualTo(REQUEST_ID)
    }

    private fun loggedRequest(level: Level): ILoggingEvent {
        val logger =
            LoggerFactory.getLogger("CallLoggingPluginInstallerTest-${UUID.randomUUID()}") as Logger
        logger.level = level
        logger.isAdditive = false
        val appender = SnapshotAppender().apply { start() }
        logger.addAppender(appender)

        try {
            testApplication {
                environment { log = logger }
                application {
                    with(CallIdPluginInstaller()) { install() }
                    with(CallLoggingPluginInstaller()) { install() }
                    routing { post("/widgets") { call.respond(HttpStatusCode.Created) } }
                }

                val response =
                    client.post("/widgets?secret=do-not-log") {
                        header(Correlation.HEADER, REQUEST_ID)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("{}")
                        header("Authorization", "Bearer sensitive")
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.Created)
            }
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }

        return appender.events.single { it.formattedMessage.contains("POST - /widgets") }
    }

    private class SnapshotAppender : AppenderBase<ILoggingEvent>() {
        val events = mutableListOf<ILoggingEvent>()

        override fun append(event: ILoggingEvent) {
            event.prepareForDeferredProcessing()
            events += event
        }
    }

    private companion object {
        const val REQUEST_ID = "test-request-id"
    }
}
