package com.rainy.myapplication.e2e

import com.rainy.myapplication.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Class rule that fails the suite up front when the dev server isn't reachable from the device.
 * `scripts/e2e-android.sh` health-checks the host side too, but IDE gutter runs bypass the script —
 * this rule turns a mid-test 15s "node never appeared" into an immediate, actionable failure.
 */
class DevServerHealthRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                assertServerReachable()
                base.evaluate()
            }
        }

    private fun assertServerReachable() {
        // Mirrors createAndroidAppGraph's base-URL choice: the dev flavor bakes DEV_SERVER_HOST
        // at build time; empty falls back to the emulator's host-loopback alias.
        val host = BuildConfig.DEV_SERVER_HOST.ifEmpty { "10.0.2.2" }
        val url = URL("http://$host:8080/health")
        try {
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = HEALTH_TIMEOUT_MILLIS
                connection.readTimeout = HEALTH_TIMEOUT_MILLIS
                val code = connection.responseCode
                if (code !in 200..299) throw AssertionError(unreachableMessage(url, "HTTP $code"))
            } finally {
                connection.disconnect()
            }
        } catch (e: IOException) {
            throw AssertionError(unreachableMessage(url, e.toString()), e)
        }
    }

    private fun unreachableMessage(url: URL, cause: String): String =
        "Dev server unreachable at $url ($cause). Start the stack with `scripts/dev-stack.sh` and " +
            "run the suite via `scripts/e2e-android.sh`. For IDE runs: build with " +
            "DEV_SERVER_HOST=10.0.2.2 (emulator) or your LAN IP (physical device)."

    private companion object {
        const val HEALTH_TIMEOUT_MILLIS = 2_000
    }
}
