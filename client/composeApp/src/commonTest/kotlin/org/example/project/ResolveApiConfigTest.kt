package org.example.project

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import org.example.project.core.buildinfo.Environment
import org.example.project.core.network.ApiConfig
import org.example.project.core.network.ApiConfigDefaults

class ResolveApiConfigTest {

    private val defaults =
        ApiConfigDefaults(
            devBaseUrl = "http://10.0.2.2:8080",
            prodBaseUrl = "https://prod.example.com",
        )

    @Test
    fun `PROD resolves to the prod build default`() {
        assertThat(resolveApiConfig(Environment.PROD, defaults))
            .isEqualTo(ApiConfig(baseUrl = "https://prod.example.com"))
    }

    @Test
    fun `DEV resolves to the platform dev default`() {
        assertThat(resolveApiConfig(Environment.DEV, defaults))
            .isEqualTo(ApiConfig(baseUrl = "http://10.0.2.2:8080"))
    }

    @Test
    fun `injected build-machine host becomes the dev default`() {
        val built =
            devApiConfigDefaults(
                injectedDevHost = "polinas-mac.local",
                fallbackDevBaseUrl = "http://10.0.2.2:8080",
            )
        assertThat(built.devBaseUrl).isEqualTo("http://polinas-mac.local:8080")
        assertThat(built.prodBaseUrl).isEqualTo(PROD_SERVER_BASE_URL)
    }

    @Test
    fun `absent or blank injected host falls back to the loopback default`() {
        assertThat(
                devApiConfigDefaults(
                        injectedDevHost = null,
                        fallbackDevBaseUrl = "http://10.0.2.2:8080",
                    )
                    .devBaseUrl
            )
            .isEqualTo("http://10.0.2.2:8080")
        assertThat(
                devApiConfigDefaults(
                        injectedDevHost = "   ",
                        fallbackDevBaseUrl = "http://localhost:8080",
                    )
                    .devBaseUrl
            )
            .isEqualTo("http://localhost:8080")
    }

    @Test
    fun `injected host that breaks origin parsing falls back`() {
        assertThat(
                devApiConfigDefaults(
                        injectedDevHost = "not a host",
                        fallbackDevBaseUrl = "http://10.0.2.2:8080",
                    )
                    .devBaseUrl
            )
            .isEqualTo("http://10.0.2.2:8080")
    }
}
