package com.rainy.myapplication.e2e

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import com.rainy.myapplication.MyApplication
import org.example.project.AndroidAppGraph

/**
 * Instrumentation entry point for the E2E suite (registered as `testInstrumentationRunner`, so the
 * Orchestrator launches it too). Installs [TestMyApplication] in place of the real Application,
 * which is how [E2eAppGraph]'s test-only accessors reach the flows without existing in a shipped
 * build.
 */
class E2eTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        classLoader: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application = super.newApplication(classLoader, TestMyApplication::class.java.name, context)
}

/** The real Application with the real launch sequence; only the graph type widens. */
class TestMyApplication : MyApplication() {
    override fun buildAppGraph(): AndroidAppGraph =
        createE2eAppGraph(
            applicationContext = this,
            environment = environment,
            devServerHost = devServerHost,
        )
}
