package org.example.project.e2e.journeys

import dev.zacsweers.metro.ClassKey
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import org.example.project.core.robots.E2eScope
import org.example.project.e2e.Journey
import org.example.project.feature.home.robots.HomeRobot

/** The app's first steps: launch, past the splash, to the landing screen. */
@Inject
@ClassKey
@ContributesIntoMap(E2eScope::class)
class ShellJourney(private val home: HomeRobot) : Journey {

    /** From a cold launch. Ends on home. */
    fun launchToHome() {
        home.awaitShown()
    }
}
