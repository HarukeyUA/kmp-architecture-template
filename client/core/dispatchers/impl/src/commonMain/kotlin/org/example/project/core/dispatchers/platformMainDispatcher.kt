package org.example.project.core.dispatchers

import kotlinx.coroutines.CoroutineDispatcher

internal expect fun platformMainDispatcher(): CoroutineDispatcher
