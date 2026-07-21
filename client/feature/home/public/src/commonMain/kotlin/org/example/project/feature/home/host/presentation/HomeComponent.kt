package org.example.project.feature.home.host.presentation

import org.example.project.core.component.AppComponentContext
import org.example.project.core.navigation.StackComponent
import org.example.project.core.ui.navigation.ScreenChild

interface HomeComponent : StackComponent<Any, ScreenChild> {

    fun interface Factory {
        fun create(componentContext: AppComponentContext): HomeComponent
    }
}
