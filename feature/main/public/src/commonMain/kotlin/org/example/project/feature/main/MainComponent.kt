package org.example.project.feature.main

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.EventComponent
import org.example.project.core.component.UiEvent
import org.example.project.core.component.snackbar.SnackbarHostState
import org.example.project.core.ui.navigation.ScreenChild

interface MainComponent : EventComponent<MainComponent.Event> {

    val snackbarHostState: SnackbarHostState

    val stack: Value<ChildStack<Tab, ScreenChild>>

    @Serializable
    sealed interface Tab {
        @Serializable data object Home : Tab

        @Serializable data object Search : Tab

        @Serializable data object Profile : Tab
    }

    sealed interface Event : UiEvent {
        object HomeTabClick : Event

        object SearchTabClick : Event

        object ProfileTabClick : Event
    }

    fun interface Factory {
        fun create(componentContext: AppComponentContext, onLogout: () -> Unit): MainComponent
    }
}
