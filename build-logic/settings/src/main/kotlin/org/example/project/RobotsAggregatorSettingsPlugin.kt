package org.example.project

import org.gradle.api.Plugin
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.getByType

/** DSL extension for configuring robots-module aggregators from `settings.gradle.kts`. */
abstract class RobotsAggregatorSettingsExtension {
    /** Full Gradle paths of the aggregator projects (e.g. `:client:androidApp`). */
    abstract val aggregatorProjectPaths: SetProperty<String>

    /**
     * Register an aggregator. It collects every leaf `:robots` module under the explicit `:client:`
     * umbrella.
     */
    fun aggregator(path: String) {
        aggregatorProjectPaths.add(path)
    }
}

/**
 * Entry point for `settings.gradle.kts`: `robotsAggregator { aggregator(":client:androidApp") }`.
 */
fun Settings.robotsAggregator(action: RobotsAggregatorSettingsExtension.() -> Unit) {
    extensions.getByType<RobotsAggregatorSettingsExtension>().action()
}

/**
 * Settings plugin that enumerates leaf `:robots` projects at settings evaluation time and writes
 * their paths into each aggregator project's own build directory.
 *
 * Walking the project tree is sanctioned at settings time -- [ProjectDescriptor] is the
 * settings-level representation of a project and carries no configuration-time state. Each file is
 * written under its aggregator's own `projectDir`, so the consuming build script can read it via
 * [org.gradle.api.file.ProjectLayout] without ever reaching into another project. That keeps the
 * whole round-trip safe under Gradle's project isolation model.
 */
class RobotsAggregatorSettingsPlugin : Plugin<Settings> {
    override fun apply(target: Settings) {
        target.extensions.create(EXTENSION_NAME, RobotsAggregatorSettingsExtension::class.java)

        target.gradle.settingsEvaluated {
            val extension = target.extensions.getByType<RobotsAggregatorSettingsExtension>()
            val aggregatorPaths = extension.aggregatorProjectPaths.orNull.orEmpty()
            require(aggregatorPaths.isNotEmpty()) {
                "`robotsAggregator` requires at least one `aggregator(path)` to be registered " +
                    "in settings.gradle.kts before settings evaluation completes."
            }

            val robotsPaths =
                target.rootProject
                    .leafDescendants()
                    .filter { it.name == "robots" && it.path.startsWith(":client:") }
                    .map { it.path }

            aggregatorPaths.forEach { aggregatorPath ->
                val aggregator =
                    runCatching { target.project(aggregatorPath) }
                        .getOrElse {
                            error(
                                "robotsAggregator: aggregator project `$aggregatorPath` is not " +
                                    "included in settings.gradle.kts."
                            )
                        }
                val outputFile = aggregator.projectDir.resolve(AGGREGATOR_OUTPUT_REL_PATH)
                outputFile.parentFile?.mkdirs()
                outputFile.writeText(robotsPaths.sorted().joinToString(separator = "\n"))
            }
        }
    }

    companion object {
        const val EXTENSION_NAME = "robotsAggregator"

        /**
         * Path of the generated file, relative to the aggregator project's `projectDir`. The
         * consuming convention plugin (`convention.robots-aggregator` in `:build-logic:convention`)
         * resolves it through [org.gradle.api.file.ProjectLayout], so no cross-project access
         * occurs at configuration time. Kept in sync by string duplication rather than a classpath
         * dependency between the settings and convention subprojects.
         */
        const val AGGREGATOR_OUTPUT_REL_PATH = "build/robots-aggregator/robots-modules.txt"
    }
}
