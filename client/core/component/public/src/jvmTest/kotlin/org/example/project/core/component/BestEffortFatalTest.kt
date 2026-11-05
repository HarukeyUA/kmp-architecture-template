package org.example.project.core.component

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import co.touchlab.kermit.Logger
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

/** JVM-only because the fatal shapes arrow refuses to catch are JVM classes. */
class BestEffortFatalTest {

    private val log = Logger.withTag("BestEffortFatalTest")

    @Test
    fun `a fatal error unwinds through an interruptible step`() = runTest {
        assertFailsWith<NoClassDefFoundError> {
            bestEffort(log, "load the driver") { throw NoClassDefFoundError("no native library") }
        }
    }

    // A driver with no native library is the documented shape of this: the step after it takes
    // keys out of memory, and nothing resumes a drain that unwound.
    @Test
    fun `the uninterruptible variant answers a fatal error as a failed step`() = runTest {
        val result =
            bestEffortUninterruptible(log, "close the database") {
                throw NoClassDefFoundError("no native library")
            }

        assertThat(result.leftOrNull()).isNotNull().isInstanceOf<NoClassDefFoundError>()
    }
}
