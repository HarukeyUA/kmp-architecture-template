package org.example.project.feature.session

import assertk.assertThat
import assertk.assertions.doesNotContain
import kotlin.test.Test

class ClientSessionToStringTest {
    @Test
    fun `neither token appears`() {
        val printed = SessionState.Bound(ClientSession("access-token", "refresh-token")).toString()

        assertThat(printed).doesNotContain("access-token")
        assertThat(printed).doesNotContain("refresh-token")
    }
}
