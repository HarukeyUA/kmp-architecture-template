package org.example.project.core.dispatchers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual fun platformMainDispatcher(): CoroutineDispatcher = Dispatchers.Main.immediate
