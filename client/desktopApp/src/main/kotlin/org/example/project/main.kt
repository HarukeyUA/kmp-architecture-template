package org.example.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import dev.zacsweers.metro.createGraphFactory
import javax.swing.SwingUtilities
import org.example.project.core.buildinfo.Environment
import org.example.project.core.component.DefaultAppComponentContext

@Suppress("TooGenericExceptionCaught")
internal fun <T> runOnUiThread(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) {
        return block()
    }

    var error: Throwable? = null
    var result: T? = null

    SwingUtilities.invokeAndWait {
        try {
            result = block()
        } catch (e: Throwable) {
            error = e
        }
    }

    error?.also { throw it }

    @Suppress("UNCHECKED_CAST")
    return result as T
}

fun main() {
    // `app.env` is baked into the launcher (and the `run` task) by the build — see
    // `-PappEnv=dev` in build.gradle.kts. The environment is decided once per process here and
    // injected, never sniffed at runtime; absent means prod, so a shipped artifact needs no flag.
    val environment =
        if (System.getProperty("app.env") == "dev") Environment.DEV else Environment.PROD

    val lifecycle = LifecycleRegistry()

    val appGraph = runOnUiThread { createGraphFactory<JvmAppGraph.Factory>().create(environment) }

    val root = runOnUiThread {
        appGraph.rootComponentFactory.create(DefaultAppComponentContext(lifecycle = lifecycle))
    }

    val rootScreen = appGraph.rootScreen

    application {
        val windowState = rememberWindowState()

        LifecycleController(lifecycle, windowState)

        Window(onCloseRequest = ::exitApplication, title = "KotlinProject") {
            App(root, rootScreen)
        }
    }
}
