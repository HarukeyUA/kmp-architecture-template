package org.example.project.feature.user.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUserRepository(isLoggedIn: Boolean = false) : UserRepository {
    private val _isLoggedIn = MutableStateFlow(isLoggedIn)
    override val isLoggedIn: Flow<Boolean> = _isLoggedIn

    /** Test hook to simulate the session being established or cleared. */
    fun setLoggedIn(value: Boolean) {
        _isLoggedIn.value = value
    }
}
