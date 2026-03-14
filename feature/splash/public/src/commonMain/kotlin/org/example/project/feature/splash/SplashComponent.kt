package org.example.project.feature.splash

import org.example.project.core.component.AppComponentContext

interface SplashComponent {
    fun interface Factory {
        fun create(
            componentContext: AppComponentContext,
            onNavigateToMain: () -> Unit,
            onNavigateToLogin: () -> Unit,
        ): SplashComponent
    }
}
