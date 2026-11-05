package org.example.project.core.error

/**
 * Wraps an [AppError] as a [Throwable] for APIs that require exceptions (e.g. Paging3's
 * LoadResult.Error). Consumers can recover the typed error via [appError].
 */
class AppErrorException(val appError: AppError) : Exception("App error: $appError")
