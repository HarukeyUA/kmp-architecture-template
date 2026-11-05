package org.example.project

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaoAwareMainThreadCheckerTest {

    private val checker = TaoAwareMainThreadChecker()

    @AfterTest
    fun resetCapturedThread() {
        TaoAwareMainThreadChecker.uiThread = null
    }

    @Test
    fun `passes while no thread is captured`() {
        TaoAwareMainThreadChecker.uiThread = null
        assertTrue(checker.isMainThread())
    }

    @Test
    fun `accepts the captured thread`() {
        TaoAwareMainThreadChecker.uiThread = Thread.currentThread()
        assertTrue(checker.isMainThread())
    }

    @Test
    fun `rejects any other thread`() {
        TaoAwareMainThreadChecker.uiThread = Thread.currentThread()
        var onOtherThread: Boolean? = null
        val thread = Thread { onOtherThread = checker.isMainThread() }
        thread.start()
        thread.join()
        assertFalse(onOtherThread!!)
    }
}
