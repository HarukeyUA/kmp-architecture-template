package org.example.project.core.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun httpClientEngine(): HttpClientEngineFactory<*> = OkHttp
