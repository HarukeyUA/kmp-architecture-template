package org.example.project.feature.home

import com.arkivanov.decompose.ComponentContext
import kotlinx.serialization.Serializable
import org.example.project.core.component.StatefulComponent
import org.example.project.core.component.UiEvent
import org.example.project.core.component.UiState

interface HomeComponent : StatefulComponent<HomeComponent.State, HomeComponent.Event> {
    @Serializable
    data class State(
        val counter: Int = 0
    ) : UiState

    sealed interface Event : UiEvent {
        data object IncrementClicked : Event
    }

    fun interface Factory {
        fun create(
            componentContext: ComponentContext
        ): HomeComponent
    }
}
