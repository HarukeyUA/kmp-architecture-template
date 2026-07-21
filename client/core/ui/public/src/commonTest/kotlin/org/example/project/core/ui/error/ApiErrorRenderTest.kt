package org.example.project.core.ui.error

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import org.example.project.client.core.ui.Res
import org.example.project.client.core.ui.error_not_found
import org.example.project.client.core.ui.error_unauthorized
import org.example.project.shared.common.NotFound
import org.example.project.shared.common.Unauthorized
import org.example.project.shared.common.UnknownApiError

/**
 * The client side of the error round-trip (ADR-0005): a known cross-cutting `ApiError` maps to its
 * localized resource, while an unknown one (a newer server's code degraded to `UnknownApiError`)
 * maps to `null` so the generic fallback in `CompositeErrorRenderer` handles it.
 */
class ApiErrorRenderTest {
    @Test
    fun `known variants map to their localized resource`() {
        assertThat(Unauthorized.toResourceResult()?.resource)
            .isEqualTo(Res.string.error_unauthorized)
        assertThat(NotFound("note").toResourceResult()?.resource)
            .isEqualTo(Res.string.error_not_found)
    }

    @Test
    fun `an unknown variant falls through to the generic fallback`() {
        assertThat(
                UnknownApiError(code = "future.teapot", status = HttpStatusCode.fromValue(418))
                    .toResourceResult()
            )
            .isNull()
    }
}
