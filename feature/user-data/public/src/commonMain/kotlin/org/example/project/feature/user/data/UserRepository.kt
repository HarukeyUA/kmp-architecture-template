package org.example.project.feature.user.data

import kotlinx.coroutines.flow.Flow

interface UserRepository {
    val isLoggedIn: Flow<Boolean>

    suspend fun setIsLoggedIn(isLoggedIn: Boolean)

    suspend fun logout()
}
