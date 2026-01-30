package org.example.project

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraph

@DependencyGraph(AppScope::class)
interface IosAppGraph : AppGraph

fun createAppGraph() = createGraph<IosAppGraph>()