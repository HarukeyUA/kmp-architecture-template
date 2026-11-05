package org.example.project

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import org.example.project.core.buildinfo.BuildType

/**
 * Configures the global Kermit [Logger] for the current build. Tagged loggers share the global
 * config by reference, so this reaches every call site whenever it was constructed. Release drops
 * the writers and raises severity, which also skips evaluating the message lambdas.
 *
 * Call once at process start, before anything logs, with the same [BuildType] the graph is given.
 */
fun configureLogging(buildType: BuildType) {
    if (buildType == BuildType.RELEASE) {
        Logger.setMinSeverity(Severity.Assert)
        Logger.setLogWriters(emptyList())
    }
}
