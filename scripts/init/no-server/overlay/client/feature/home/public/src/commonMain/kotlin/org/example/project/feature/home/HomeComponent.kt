package org.example.project.feature.home

import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.StatefulComponent
import org.example.project.core.component.UiState

/** The landing screen: a placeholder for the app's content. */
interface HomeComponent : StatefulComponent<HomeComponent.State> {
    data object State : UiState

    fun interface Factory {
        fun create(componentContext: AppComponentContext): HomeComponent
    }
}
