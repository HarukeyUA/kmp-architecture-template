package org.example.project.feature.session

sealed interface SessionRepositoryError {
    data class Storage(val cause: Throwable) : SessionRepositoryError
}
