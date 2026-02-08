package org.example.project.feature.user.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUserRepository(isLoggedIn: Boolean = false) : UserRepository {
    private val _isLoggedIn = MutableStateFlow(isLoggedIn)
    override val isLoggedIn: Flow<Boolean> = _isLoggedIn

    var setIsLoggedInCalled = false
        private set

    var logoutCalled = false
        private set

    override suspend fun setIsLoggedIn(isLoggedIn: Boolean) {
        setIsLoggedInCalled = true
        _isLoggedIn.value = isLoggedIn
    }

    override suspend fun logout() {
        logoutCalled = true
        _isLoggedIn.value = false
    }
}
