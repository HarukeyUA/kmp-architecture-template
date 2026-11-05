package org.example.project.e2e

import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provider
import kotlin.reflect.KClass
import org.example.project.core.robots.Robot

/**
 * What every host's `E2eScope` graph exposes: the robots and journeys contributed by class. Each
 * lookup builds a fresh driver, so nothing resolved here carries state between calls.
 */
interface E2eDrivers {
    @Multibinds val robots: Map<KClass<*>, Provider<Robot>>

    @Multibinds val journeys: Map<KClass<*>, Provider<Journey>>
}

inline fun <reified T : Robot> E2eDrivers.robot(): T = robots.provide(T::class) as T

inline fun <reified T : Journey> E2eDrivers.journey(): T = journeys.provide(T::class) as T

@PublishedApi
internal fun Map<KClass<*>, Provider<out Any>>.provide(type: KClass<*>): Any =
    requireNotNull(this[type]) { "$type is not contributed to E2eScope" }()
