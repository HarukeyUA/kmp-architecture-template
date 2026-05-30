package org.example.project.core.component

import arrow.core.Either
import org.example.project.core.error.AppError

fun <E : AppError, T> Either<E, T>.toLoadable(): Loadable<T> =
    fold(ifLeft = { Loadable.Failed(it) }, ifRight = { Loadable.Loaded(it) })
