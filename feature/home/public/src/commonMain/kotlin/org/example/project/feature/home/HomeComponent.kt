package org.example.project.feature.home

import com.arkivanov.decompose.ComponentContext
import org.example.project.core.navigation.StackComponent

interface HomeComponent : StackComponent<Any, HomeComponent.Child> {

    sealed interface Child {
        data class List(val component: HomeListComponent) : Child

        data class Detail(val component: HomeDetailComponent) : Child
    }

    fun interface Factory {
        fun create(componentContext: ComponentContext): HomeComponent
    }
}
