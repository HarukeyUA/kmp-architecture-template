package org.example.project.feature.user.data

import kotlinx.coroutines.flow.Flow

/**
 * Read-only session state used to route the app: `true` while a Session is held in secure storage.
 * Mutations live in `AuthRepository` (login/signup/logout); clearing the session — by logout or by
 * the HttpClient's 401 interceptor — flips this to `false`, which the root navigation observes.
 */
interface UserRepository {
    val isLoggedIn: Flow<Boolean>
}
