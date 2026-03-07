package org.example.project.feature.profile

import kotlinx.serialization.Serializable
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.StatefulComponent
import org.example.project.core.component.UiEvent
import org.example.project.core.component.UiState

interface ProfileComponent : StatefulComponent<ProfileComponent.State, ProfileComponent.Event> {
    @Serializable
    data class State(val userName: String = "User", val email: String = "user@example.com") :
        UiState

    sealed interface Event : UiEvent {
        data object LogoutClicked : Event
    }

    fun interface Factory {
        fun create(componentContext: AppComponentContext, onLogout: () -> Unit): ProfileComponent
    }
}
