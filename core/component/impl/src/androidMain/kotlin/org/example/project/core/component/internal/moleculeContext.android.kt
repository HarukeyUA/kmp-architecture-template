package org.example.project.core.component.internal

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

internal actual fun moleculeContext(): CoroutineContext = Dispatchers.Main.immediate
