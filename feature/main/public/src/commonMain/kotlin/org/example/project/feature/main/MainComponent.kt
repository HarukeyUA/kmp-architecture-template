package org.example.project.feature.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import org.example.project.core.component.EventComponent
import org.example.project.core.component.UiEvent
import org.example.project.feature.home.HomeComponent
import org.example.project.feature.profile.ProfileComponent
import org.example.project.feature.search.SearchComponent

interface MainComponent : EventComponent<MainComponent.Event> {

    val stack: Value<ChildStack<*, Child>>

    sealed interface Child {
        data class Home(val component: HomeComponent) : Child

        data class Search(val component: SearchComponent) : Child

        data class Profile(val component: ProfileComponent) : Child
    }

    sealed interface Event : UiEvent {
        object HomeTabClick : Event

        object SearchTabClick : Event

        object ProfileTabClick : Event
    }

    fun interface Factory {
        fun create(componentContext: ComponentContext, onLogout: () -> Unit): MainComponent
    }
}
