package com.rainy.myapplication.e2e

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.test.services.storage.TestStorage
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Captures the compose root on test failure into androidx.test.services storage, which survives the
 * Orchestrator's per-test `pm clear` and is pulled into
 * `client/androidApp/build/outputs/.../connected_android_test_additional_output` by the connected
 * task. Must be wrapped *inside* the compose rule (see `E2eTest`'s RuleChain) so the compose host
 * is still alive when the capture runs.
 */
class FailureScreenshotRule(private val nodes: SemanticsNodeInteractionsProvider) : TestWatcher() {
    override fun failed(e: Throwable, description: Description) {
        runCatching {
                val screenshot = nodes.onRoot().captureToImage().asAndroidBitmap()
                val name = "${description.className}.${description.methodName}.png"
                TestStorage().openOutputFile(name).use { stream ->
                    screenshot.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
            }
            .onFailure { Log.e(TAG, "Failed to capture failure screenshot", it) }
    }

    private companion object {
        const val TAG = "FailureScreenshotRule"
    }
}
