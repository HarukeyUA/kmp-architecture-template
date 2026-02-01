package org.example.project

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import org.example.project.core.component.LoggedInScope
import org.example.project.feature.main.MainComponent

@GraphExtension(LoggedInScope::class)
interface LoggedInGraph {
    val mainComponentFactory: MainComponent.Factory

    @ContributesTo(AppScope::class)
    @GraphExtension.Factory
    interface Factory {
        fun create(): LoggedInGraph
    }
}
