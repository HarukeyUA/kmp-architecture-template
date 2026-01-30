package org.example.project.feature.auth

import com.arkivanov.decompose.ComponentContext
import kotlinx.serialization.Serializable
import org.example.project.core.component.StatefulComponent
import org.example.project.core.component.UiEvent
import org.example.project.core.component.UiState

interface LoginComponent : StatefulComponent<LoginComponent.State, LoginComponent.Event> {
    @Serializable
    data class State(
        val counter: Int = 0
    ) : UiState

    sealed interface Event : UiEvent {
        data object LoginClicked : Event
    }

    fun interface Factory {
        fun create(
            componentContext: ComponentContext,
            onLoginSuccess: () -> Unit
        ): LoginComponent
    }
}
