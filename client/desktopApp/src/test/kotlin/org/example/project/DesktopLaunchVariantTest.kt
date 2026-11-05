package org.example.project

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import org.example.project.core.buildinfo.BuildType
import org.example.project.core.buildinfo.Environment

class DesktopLaunchVariantTest {

    @Test
    fun `the dev suffix is what names the dev environment`() {
        assertThat(environmentOf("$DEFAULT_APP_ID.dev")).isEqualTo(Environment.DEV)
    }

    @Test
    fun `every other app id is prod, the fallback included`() {
        assertThat(environmentOf(DEFAULT_APP_ID)).isEqualTo(Environment.PROD)
        assertThat(environmentOf("$DEFAULT_APP_ID.develop")).isEqualTo(Environment.PROD)
    }

    @Test
    fun `a dev launcher and an unmanaged launch are debug builds`() {
        assertThat(buildTypeOf("$DEFAULT_APP_ID.dev")).isEqualTo(BuildType.DEBUG)
        assertThat(buildTypeOf(null)).isEqualTo(BuildType.DEBUG)
    }

    @Test
    fun `only a baked prod id is a release build`() {
        assertThat(buildTypeOf(DEFAULT_APP_ID)).isEqualTo(BuildType.RELEASE)
    }
}
