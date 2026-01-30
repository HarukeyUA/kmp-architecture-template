package org.example.project.feature.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.value.Value
import org.example.project.core.navigation.TabbedComponent
import org.example.project.feature.home.HomeComponent
import org.example.project.feature.profile.ProfileComponent
import org.example.project.feature.search.SearchComponent

interface MainComponent : TabbedComponent<Any, MainComponent.Child> {
    sealed interface Child {
        data class Home(val component: HomeComponent) : Child
        data class Search(val component: SearchComponent) : Child
        data class Profile(val component: ProfileComponent) : Child
    }

    fun interface Factory {
        fun create(
            componentContext: ComponentContext,
            onLogout: () -> Unit
        ): MainComponent
    }
}
