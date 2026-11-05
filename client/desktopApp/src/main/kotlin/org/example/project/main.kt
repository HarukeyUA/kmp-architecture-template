package org.example.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import co.touchlab.kermit.Logger
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import dev.nucleusframework.application.NucleusBackend
import dev.nucleusframework.application.nucleusApplication
import dev.nucleusframework.core.runtime.SingleInstanceManager
import dev.nucleusframework.window.material.MaterialDecoratedWindow
import dev.nucleusframework.window.material.MaterialTitleBar
import dev.zacsweers.metro.createGraphFactory
import io.github.vinceglb.filekit.FileKit
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess
import org.example.project.core.buildinfo.BuildType
import org.example.project.core.buildinfo.Environment
import org.example.project.core.component.DefaultAppComponentContext
import org.example.project.core.ui.theme.AppTheme
import org.example.project.core.ui.theme.resolveDarkTheme

fun main(args: Array<String>) {
    // Baked into the launcher by the build and read directly, because Nucleus's `AppIdProvider`
    // falls back to other ids on an unmanaged launch and would relocate the data directory.
    val bakedAppId = System.getProperty("nucleus.app.id")?.takeIf { it.isNotBlank() }
    val appId = bakedAppId ?: DEFAULT_APP_ID

    // Before the first log line and the graph below.
    configureLogging(buildTypeOf(bakedAppId))
    Logger.i { "desktop app id: $appId" }

    installCrashHandler()

    // `nucleusApplication` turns this thread into the Tao event loop.
    TaoAwareMainThreadChecker.uiThread = Thread.currentThread()

    // Picks the per-platform data directory behind `FileKit.filesDir`.
    FileKit.init(appId = appId)

    // Must precede `nucleusApplication`, which acquires the lock.
    SingleInstanceManager.configuration =
        SingleInstanceManager.Configuration(lockIdentifier = appId)

    val lifecycle = LifecycleRegistry()
    val appGraph = createGraphFactory<JvmAppGraph.Factory>().create(environmentOf(appId))
    val rootScreen = appGraph.rootScreen

    nucleusApplication(args = args, backend = NucleusBackend.Tao) {
        // Built in composition because its `Dispatchers.Main` coroutines need the live Tao loop:
        // started any earlier they run on a background thread and trip the main-thread check.
        val root = remember {
            appGraph.rootComponentFactory.create(DefaultAppComponentContext(lifecycle = lifecycle))
        }

        val rootState by root.state.collectAsState()

        val windowState = rememberWindowState(size = DEFAULT_WINDOW_SIZE)

        LifecycleController(lifecycle, windowState)

        // Styles the title bar only: on Tao, CompositionLocals don't cross into the window's own
        // ComposeScene, so `App` applies the same theme again inside it.
        AppTheme(darkTheme = rootState.themeMode.resolveDarkTheme(), dynamicColorEnabled = true) {
            MaterialDecoratedWindow(
                onCloseRequest = ::exitApplication,
                state = windowState,
                title = WINDOW_TITLE,
                minimumSize = MIN_WINDOW_SIZE,
            ) {
                MaterialTitleBar { _ ->
                    Text(
                        text = WINDOW_TITLE,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }

                App(root, rootScreen, dynamicColorEnabled = true)
            }
        }
    }
}

/** The `.dev` suffix that isolates the data directory also points the build at a local server. */
internal fun environmentOf(appId: String): Environment =
    if (appId.endsWith(".dev")) Environment.DEV else Environment.PROD

/** Only a packaged prod launcher is release: `gradlew run` and the IDE have a console. */
internal fun buildTypeOf(bakedAppId: String?): BuildType =
    if (bakedAppId != null && environmentOf(bakedAppId) == Environment.PROD) BuildType.RELEASE
    else BuildType.DEBUG

/** What an unmanaged launch keys its data directory by. */
internal const val DEFAULT_APP_ID = "org.example.project"

private val crashing = AtomicBoolean(false)

// Composition failures reach this handler only because Nucleus's Tao application scope installs no
// `CoroutineExceptionHandler`; this path silently disappears if Nucleus ever adds one.
private fun installCrashHandler() {
    Thread.setDefaultUncaughtExceptionHandler { thread, error -> handleFatal(thread, error) }
}

private fun handleFatal(thread: Thread, error: Throwable) {
    if (!crashing.compareAndSet(false, true)) return
    System.err.println("Fatal uncaught exception on ${thread.name}")
    error.printStackTrace()
    runCatching {
        val message = buildString {
            append("$WINDOW_TITLE encountered an unexpected error and must close.\n\n")
            append(error.javaClass.simpleName)
            error.message?.takeIf { it.isNotBlank() }?.let { append(": ").append(it) }
        }
            .take(FATAL_MESSAGE_MAX_LENGTH)
        showFatalAlert(message)
    }
    exitProcess(1)
}

// An external process, because the Tao backend never builds the window hierarchy `JOptionPane`
// needs. With no helper available this degrades to the stderr log in `handleFatal`.
private fun showFatalAlert(message: String) {
    for (command in fatalAlertCommands(message)) {
        val shown = runCatching {
            val process = ProcessBuilder(command).start()
            if (!process.waitFor(FATAL_ALERT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly()
            }
            true
        }
            .onFailure { System.err.println("fatal-alert helper ${command.first()} failed: $it") }
            .getOrDefault(false)
        if (shown) return
    }
}

private fun fatalAlertCommands(message: String): List<List<String>> {
    val os = System.getProperty("os.name").orEmpty().lowercase()
    return when {
        os.contains("mac") ->
            listOf(
                listOf(
                    "osascript",
                    "-e",
                    "display alert \"$WINDOW_TITLE\" message " +
                        "\"${message.escapedForAppleScript()}\" as critical",
                ),
            )
        os.contains("linux") ->
            listOf(
                // `--no-markup`: exception messages routinely contain `<...>`, which zenity would
                // otherwise parse as Pango markup and garble or reject.
                listOf(
                    "zenity",
                    "--error",
                    "--no-markup",
                    "--title=$WINDOW_TITLE",
                    "--text=$message",
                ),
                listOf("kdialog", "--error", message, "--title", WINDOW_TITLE),
            )
        os.contains("windows") ->
            listOf(
                listOf(
                    "powershell",
                    "-NoProfile",
                    "-Command",
                    "Add-Type -AssemblyName System.Windows.Forms; " +
                        "[System.Windows.Forms.MessageBox]::Show(" +
                        "'${message.escapedForPowerShell()}', '$WINDOW_TITLE', 'OK', 'Error')",
                ),
            )
        else -> emptyList()
    }
}

private fun String.escapedForAppleScript(): String = replace("\\", "\\\\").replace("\"", "\\\"")

private fun String.escapedForPowerShell(): String = replace("'", "''")

private const val WINDOW_TITLE = "Template"
private val DEFAULT_WINDOW_SIZE = DpSize(1100.dp, 720.dp)
// Logical pixels; `MaterialDecoratedWindow` applies the floor natively and handles HiDPI scaling.
private val MIN_WINDOW_SIZE = DpSize(720.dp, 480.dp)
private const val FATAL_MESSAGE_MAX_LENGTH = 800
private const val FATAL_ALERT_TIMEOUT_SECONDS = 60L
