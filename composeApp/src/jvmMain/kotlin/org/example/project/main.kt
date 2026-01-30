package org.example.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import dev.zacsweers.metro.createGraph
import javax.swing.SwingUtilities

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
    val lifecycle = LifecycleRegistry()

    val appGraph = runOnUiThread { createGraph<JvmAppGraph>() }

    val root = runOnUiThread {
        appGraph.rootComponentFactory.create(DefaultComponentContext(lifecycle = lifecycle))
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
