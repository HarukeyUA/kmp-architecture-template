package org.example.project.e2e

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import org.example.project.AndroidAppGraph
import org.example.project.TemplateApplication

/**
 * Instrumentation entry point for the E2E suite (registered as `testInstrumentationRunner`, so the
 * Orchestrator launches it too). Installs [TestTemplateApplication] in place of the real
 * Application, which is how [E2eAppGraph]'s test-only accessors reach the flows without existing in
 * a shipped build.
 */
class E2eTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        classLoader: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application =
        super.newApplication(classLoader, TestTemplateApplication::class.java.name, context)
}

/** The real Application with the real launch sequence; only the graph type widens. */
class TestTemplateApplication : TemplateApplication() {
    override fun buildAppGraph(): AndroidAppGraph =
        createE2eAppGraph(
            applicationContext = this,
            environment = environment,
            devServerHost = devServerHost,
        )
}
