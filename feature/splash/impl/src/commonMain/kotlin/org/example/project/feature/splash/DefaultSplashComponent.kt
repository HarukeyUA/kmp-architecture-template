package org.example.project.feature.splash

import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.example.project.core.component.AppComponentContext
import org.example.project.feature.user.data.UserRepository

@AssistedInject
class DefaultSplashComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted private val onNavigateToMain: () -> Unit,
    @Assisted private val onNavigateToLogin: () -> Unit,
    private val userRepository: UserRepository,
) : SplashComponent, AppComponentContext by componentContext {
    private val coroutineScope = coroutineScope()

    init {
        coroutineScope.launch {
            val isLoggedIn = userRepository.isLoggedIn.first()
            if (isLoggedIn) onNavigateToMain() else onNavigateToLogin()
        }
    }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : SplashComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            onNavigateToMain: () -> Unit,
            onNavigateToLogin: () -> Unit,
        ): DefaultSplashComponent
    }
}
