package org.example.project.core.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.SnapshotNotifier
import app.cash.molecule.moleculeFlow
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.backhandler.BackEvent
import com.arkivanov.essenty.backhandler.BackHandler
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import kotlin.test.Test
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class HandleBackTest {

    @Test
    fun `back runs the lambda from the latest composition, not the first`() = runTest {
        val dispatcher = BackDispatcher()
        val owner = backHandlerOwner(dispatcher)
        var pressedGeneration: Int? = null
        var generation by mutableStateOf(1)

        moleculeFlow(
                mode = RecompositionMode.Immediate,
                snapshotNotifier = SnapshotNotifier.WhileActive,
            ) {
                // Read in the composition, so each generation hands HandleBack a distinct closure.
                val composed = generation
                with(owner) { HandleBack(onBack = { pressedGeneration = composed }) }
            }
            .launchIn(backgroundScope)
        runCurrent()

        generation = 2
        runCurrent()
        dispatcher.back()

        assertThat(pressedGeneration).isEqualTo(2)
    }

    @Test
    fun `a predictive gesture reaches the latest gesture lambdas`() = runTest {
        val dispatcher = BackDispatcher()
        val owner = backHandlerOwner(dispatcher)
        val seen = mutableListOf<String>()
        var generation by mutableStateOf(1)

        moleculeFlow(
                mode = RecompositionMode.Immediate,
                snapshotNotifier = SnapshotNotifier.WhileActive,
            ) {
                val composed = generation
                with(owner) {
                    HandleBack(
                        onBackStarted = { seen += "started $composed" },
                        onBackProgressed = { seen += "progressed $composed" },
                        onBackCancelled = { seen += "cancelled $composed" },
                        onBack = {},
                    )
                }
            }
            .launchIn(backgroundScope)
        runCurrent()

        generation = 2
        runCurrent()
        dispatcher.startPredictiveBack(BackEvent())
        dispatcher.progressPredictiveBack(BackEvent(progress = 0.5f))
        dispatcher.cancelPredictiveBack()

        assertThat(seen).isEqualTo(listOf("started 2", "progressed 2", "cancelled 2"))
    }

    private fun backHandlerOwner(handler: BackHandler): BackHandlerOwner =
        object : BackHandlerOwner {
            override val backHandler: BackHandler = handler
        }
}
