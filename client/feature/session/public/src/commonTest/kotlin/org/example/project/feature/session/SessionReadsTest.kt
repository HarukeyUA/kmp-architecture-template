package org.example.project.feature.session

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class SessionReadsTest {
    private val log = Logger.withTag("SessionReadsTest")
    private val session = ClientSession("access", "refresh")
    private val unreadable = SessionRepositoryError.Storage(IllegalStateException("disk gone"))

    @Test
    fun `stateOrNull passes every state through and nulls a failed read`() = runTest {
        assertThat(ScriptedStatus(SessionState.Unbound.right()).stateOrNull(log))
            .isEqualTo(SessionState.Unbound)
        assertThat(ScriptedStatus(SessionState.Bound(session).right()).stateOrNull(log))
            .isEqualTo(SessionState.Bound(session))
        assertThat(ScriptedStatus(unreadable.left()).stateOrNull(log)).isNull()
    }

    @Test
    fun `stateOrNull logs the failed read with its cause`() = runTest {
        val writer = RecordingLogWriter()
        val log = Logger(StaticConfig(logWriterList = listOf(writer)), "SessionReadsTest")

        ScriptedStatus(unreadable.left()).stateOrNull(log)

        assertThat(writer.throwables.single()).isSameInstanceAs(unreadable.cause)
    }

    @Test
    fun `boundSessionOrNull answers the session only when bound`() = runTest {
        assertThat(ScriptedStatus(SessionState.Bound(session).right()).boundSessionOrNull(log))
            .isEqualTo(session)
        assertThat(ScriptedStatus(SessionState.Unbound.right()).boundSessionOrNull(log)).isNull()
        assertThat(ScriptedStatus(unreadable.left()).boundSessionOrNull(log)).isNull()
    }

    private class RecordingLogWriter : LogWriter() {
        val throwables = mutableListOf<Throwable?>()

        override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
            throwables += throwable
        }
    }

    private class ScriptedStatus(private val answer: Either<SessionRepositoryError, SessionState>) :
        SessionStatus {
        override fun observe(): Flow<SessionState> = flowOf()

        override suspend fun snapshot(): Either<SessionRepositoryError, SessionState> = answer
    }
}
