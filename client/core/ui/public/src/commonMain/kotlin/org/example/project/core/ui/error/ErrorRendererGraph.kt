package org.example.project.core.ui.error

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import org.example.project.core.error.AppError

@ContributesTo(AppScope::class)
interface ErrorRendererGraph {
    @Multibinds fun errorRenderers(): Set<ErrorRenderer<AppError>>
}
