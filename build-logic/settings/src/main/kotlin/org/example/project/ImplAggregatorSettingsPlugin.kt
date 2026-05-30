package org.example.project

import org.gradle.api.Plugin
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.getByType

/** DSL extension for configuring impl-module aggregators from `settings.gradle.kts`. */
abstract class ImplAggregatorSettingsExtension {
    /** Full Gradle paths of the aggregator projects (e.g. `:client:composeApp`, `:server:app`). */
    abstract val aggregatorProjectPaths: SetProperty<String>

    /**
     * Register an aggregator. It collects every leaf `:impl` module **within its own umbrella** —
     * the umbrella is the aggregator path's first segment (`:client:composeApp` → `:client:*:impl`,
     * `:server:app` → `:server:*:impl`). This keeps client and server contributions from leaking
     * across the umbrella boundary (ADR-0001).
     */
    fun aggregator(path: String) {
        aggregatorProjectPaths.add(path)
    }
}

/** Entry point for `settings.gradle.kts`: `implAggregator { aggregator(":client:composeApp") }`. */
fun Settings.implAggregator(action: ImplAggregatorSettingsExtension.() -> Unit) {
    extensions.getByType<ImplAggregatorSettingsExtension>().action()
}

/**
 * Settings plugin that enumerates leaf `:impl` projects at settings evaluation time and writes
 * their paths into each aggregator project's own build directory, scoped to that aggregator's
 * umbrella.
 *
 * Walking the project tree is sanctioned at settings time -- [ProjectDescriptor] is the
 * settings-level representation of a project and carries no configuration-time state. Each file is
 * written under its aggregator's own `projectDir`, so the consuming build script can read it via
 * [org.gradle.api.file.ProjectLayout] without ever reaching into another project. That keeps the
 * whole round-trip safe under Gradle's project isolation model.
 */
class ImplAggregatorSettingsPlugin : Plugin<Settings> {
    override fun apply(target: Settings) {
        target.extensions.create(EXTENSION_NAME, ImplAggregatorSettingsExtension::class.java)

        target.gradle.settingsEvaluated {
            val extension = target.extensions.getByType<ImplAggregatorSettingsExtension>()
            val aggregatorPaths = extension.aggregatorProjectPaths.orNull.orEmpty()
            require(aggregatorPaths.isNotEmpty()) {
                "`implAggregator` requires at least one `aggregator(path)` to be registered " +
                    "in settings.gradle.kts before settings evaluation completes."
            }

            val allImplPaths =
                target.rootProject.leafDescendants().filter { it.name == "impl" }.map { it.path }

            aggregatorPaths.forEach { aggregatorPath ->
                val aggregator =
                    runCatching { target.project(aggregatorPath) }
                        .getOrElse {
                            error(
                                "Aggregator project `$aggregatorPath` is not included in settings.gradle.kts."
                            )
                        }
                val umbrellaPrefix = umbrellaPrefixOf(aggregatorPath)
                val paths = allImplPaths.filter { it.startsWith(umbrellaPrefix) }
                val outputFile = aggregator.projectDir.resolve(AGGREGATOR_OUTPUT_REL_PATH)
                outputFile.parentFile?.mkdirs()
                outputFile.writeText(paths.sorted().joinToString(separator = "\n"))
            }
        }
    }

    /** `:client:composeApp` -> `:client:`, `:server:app` -> `:server:`. */
    private fun umbrellaPrefixOf(aggregatorPath: String): String =
        ":" + aggregatorPath.removePrefix(":").substringBefore(":") + ":"

    companion object {
        const val EXTENSION_NAME = "implAggregator"

        /**
         * Path of the generated file, relative to the aggregator project's `projectDir`. The
         * consuming convention plugin resolves it through [org.gradle.api.file.ProjectLayout], so
         * no cross-project access occurs at configuration time.
         */
        const val AGGREGATOR_OUTPUT_REL_PATH = "build/impl-aggregator/impl-modules.txt"
    }
}
