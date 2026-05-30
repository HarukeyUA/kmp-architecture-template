package org.example.project.core.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

internal actual fun httpClientEngine(): HttpClientEngineFactory<*> = Darwin
