package org.example.project

import android.app.Application
import org.example.project.core.buildinfo.BuildType
import org.example.project.core.buildinfo.Environment

open class TemplateApplication : Application() {
    val appGraph: AndroidAppGraph by lazy { buildAppGraph() }

    /**
     * The one seam the instrumented E2E runner replaces: it installs an Application subclass that
     * builds a graph subtype carrying accessors only the flow tests need, which keeps those
     * accessors out of every shipped build.
     */
    protected open fun buildAppGraph(): AndroidAppGraph =
        createAndroidAppGraph(
            applicationContext = this,
            environment = environment,
            devServerHost = devServerHost,
        )

    /** The flavor picked at install time decides which server this install talks to. */
    protected val environment: Environment
        get() = if (BuildConfig.FLAVOR == "dev") Environment.DEV else Environment.PROD

    /** The build machine's LAN IP, baked into the dev flavor; null in prod and offline builds. */
    protected val devServerHost: String?
        get() = BuildConfig.DEV_SERVER_HOST.takeIf { it.isNotEmpty() }

    override fun onCreate() {
        super.onCreate()
        configureLogging(if (BuildConfig.DEBUG) BuildType.DEBUG else BuildType.RELEASE)
    }
}
