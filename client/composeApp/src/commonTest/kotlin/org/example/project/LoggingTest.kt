package org.example.project

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEmpty
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import kotlin.test.Test
import org.example.project.core.buildinfo.BuildType

/**
 * The desktop launcher has no release build to silence it, so this is the only thing standing
 * between a prod launch and request lines and session diagnostics on stdout.
 */
class LoggingTest {

    @Test
    fun `a release build writes nothing anywhere`() {
        val recorder = withRecordedLogging(BuildType.RELEASE) { Logger.e { "a credential" } }

        assertThat(recorder.lines, "a release build still had a writer to log through").isEmpty()
    }

    @Test
    fun `a debug build keeps its writers`() {
        val recorder = withRecordedLogging(BuildType.DEBUG) { Logger.i { "a request line" } }

        assertThat(recorder.lines).contains("a request line")
    }

    // Kermit's config is process-global and every other test in this module shares it, so the whole
    // of it is put back before this returns.
    private fun withRecordedLogging(buildType: BuildType, log: () -> Unit): RecordingLogWriter {
        val restoreWriters = Logger.config.logWriterList
        val restoreSeverity = Logger.config.minSeverity
        val recorder = RecordingLogWriter()
        return try {
            Logger.setLogWriters(recorder)
            Logger.setMinSeverity(Severity.Verbose)
            configureLogging(buildType)
            log()
            recorder
        } finally {
            Logger.setLogWriters(restoreWriters)
            Logger.setMinSeverity(restoreSeverity)
        }
    }

    private class RecordingLogWriter : LogWriter() {
        val lines: MutableList<String> = mutableListOf()

        override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
            lines += message
        }
    }
}
