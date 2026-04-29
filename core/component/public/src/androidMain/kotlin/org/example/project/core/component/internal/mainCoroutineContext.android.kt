package org.example.project.core.component.internal

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

internal actual fun mainCoroutineContext(): CoroutineContext = Dispatchers.Main.immediate
