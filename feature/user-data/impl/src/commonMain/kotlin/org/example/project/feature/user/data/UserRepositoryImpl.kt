package org.example.project.feature.user.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.map
import org.example.project.core.local.storage.SettingsLocalDataSource

@Inject
@ContributesBinding(AppScope::class)
class UserRepositoryImpl(private val settingsLocalDataSource: SettingsLocalDataSource) :
    UserRepository {
    override val isLoggedIn = settingsLocalDataSource.settingsFlow.map { it.isLoggedIn }

    override suspend fun setIsLoggedIn(isLoggedIn: Boolean) {
        settingsLocalDataSource.updateSettings { settings ->
            settings.copy(isLoggedIn = isLoggedIn)
        }
    }

    override suspend fun logout() {
        settingsLocalDataSource.reset()
    }
}
