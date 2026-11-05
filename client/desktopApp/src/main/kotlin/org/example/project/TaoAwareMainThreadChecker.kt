package org.example.project

import com.arkivanov.decompose.InternalDecomposeApi
import com.arkivanov.decompose.mainthread.MainThreadChecker

/**
 * Tao's UI thread is the process main thread, not the AWT EDT that Decompose's bundled Swing
 * checker tests for. Decompose loads the first `META-INF/services` provider, so the ProGuard build
 * strips the Swing entry from dependency jars. While [uiThread] is unset (tests) the check passes.
 */
@OptIn(InternalDecomposeApi::class)
class TaoAwareMainThreadChecker : MainThreadChecker {

    override fun isMainThread(): Boolean {
        val expected = uiThread ?: return true
        return Thread.currentThread() === expected
    }

    companion object {
        @Volatile var uiThread: Thread? = null
    }
}
