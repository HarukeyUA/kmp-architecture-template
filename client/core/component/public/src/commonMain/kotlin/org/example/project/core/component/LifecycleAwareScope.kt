package org.example.project.core.component

import com.arkivanov.essenty.lifecycle.LifecycleOwner
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.example.project.core.component.internal.mainCoroutineContext

/**
 * Creates a [CoroutineScope] tied to this [LifecycleOwner] that runs on the platform main
 * dispatcher and uses a [SupervisorJob] so a single child failure does not cancel siblings.
 *
 * The scope is canceled automatically when the lifecycle is destroyed.
 */
fun LifecycleOwner.lifecycleAwareScope(): CoroutineScope =
    coroutineScope(mainCoroutineContext() + SupervisorJob())
