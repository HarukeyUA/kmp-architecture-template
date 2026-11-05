package org.example.project.core.dispatchers

import dev.zacsweers.metro.Qualifier

/**
 * Qualifier for the app-wide [kotlinx.coroutines.CoroutineScope] used to launch operations that
 * outlive any single component. The scope uses a [kotlinx.coroutines.SupervisorJob] so a single
 * failure does not cancel sibling jobs.
 */
@Qualifier annotation class ApplicationCoroutineScope
