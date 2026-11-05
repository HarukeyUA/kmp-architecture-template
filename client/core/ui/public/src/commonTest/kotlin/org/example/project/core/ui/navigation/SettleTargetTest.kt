package org.example.project.core.ui.navigation

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import kotlin.test.Test

class SettleTargetTest {

    @Test
    fun `a released gesture carries on to where it already was`() {
        assertThat(settleTarget(currentProgress = 0.4f, velocity = 0f)).isEqualTo(0.4f)
    }

    @Test
    fun `an unhurried gesture covers its share of what is left`() {
        assertThat(settleTarget(currentProgress = 0.4f, velocity = 0.5f)).isCloseTo(0.7f, 1e-6f)
    }

    @Test
    fun `a fling stops at the far edge instead of overshooting it`() {
        assertThat(settleTarget(currentProgress = 0.4f, velocity = 5f)).isEqualTo(1f)
    }

    @Test
    fun `a backwards flick never drags the committed gesture into reverse`() {
        assertThat(settleTarget(currentProgress = 0.4f, velocity = -2f)).isEqualTo(0.4f)
    }
}
