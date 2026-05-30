package org.example.project.feature.user.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.example.project.core.secure.storage.SecureSessionStore

@Inject
@ContributesBinding(AppScope::class)
class UserRepositoryImpl(secureSessionStore: SecureSessionStore) : UserRepository {
    override val isLoggedIn: Flow<Boolean> = secureSessionStore.sessionFlow().map { it != null }
}
