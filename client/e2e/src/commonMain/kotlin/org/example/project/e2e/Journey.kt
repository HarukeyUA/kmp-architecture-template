package org.example.project.e2e

/**
 * Marker for a flow-level driver: sequences robots, and other journeys, across screens the way the
 * nav graph joins them. Holds no state and owns no resources: what a later step needs comes back as
 * a return value, and anything below the UI arrives as a parameter.
 */
interface Journey
