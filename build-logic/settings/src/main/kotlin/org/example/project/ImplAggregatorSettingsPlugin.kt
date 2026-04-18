package org.example.project

import org.gradle.api.Plugin
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.getByType

/** DSL extension for configuring the impl-module aggregator from `settings.gradle.kts`. */
abstract class ImplAggregatorSettingsExtension {
    /** Full Gradle path of the aggregator project (e.g. `:composeApp`). */
    abstract val aggregatorProjectPath: Property<String>
}

/** Entry point for `settings.gradle.kts`: `implAggregator { aggregatorProjectPath.set(...) }`. */
fun Settings.implAggregator(action: ImplAggregatorSettingsExtension.() -> Unit) {
    extensions.getByType<ImplAggregatorSettingsExtension>().action()
}

/**
 * Settings plugin that enumerates every leaf `:impl` project at settings evaluation time and writes
 * their paths into the aggregator project's own build directory.
 *
 * Walking the project tree is sanctioned at settings time -- [ProjectDescriptor] is the
 * settings-level representation of a project and carries no configuration-time state. The file is
 * written under the aggregator's own `projectDir`, so its consuming build script can read it via
 * [org.gradle.api.file.ProjectLayout] without ever reaching into another project. That keeps the
 * whole round-trip safe under Gradle's project isolation model.
 */
class ImplAggregatorSettingsPlugin : Plugin<Settings> {
    override fun apply(target: Settings) {
        target.extensions.create(EXTENSION_NAME, ImplAggregatorSettingsExtension::class.java)

        target.gradle.settingsEvaluated {
            val extension = target.extensions.getByType<ImplAggregatorSettingsExtension>()
            val aggregatorPath =
                extension.aggregatorProjectPath.orNull
                    ?: error(
                        "`implAggregator.aggregatorProjectPath` must be set in settings.gradle.kts " +
                            "before settings evaluation completes."
                    )
            val aggregator =
                runCatching { target.project(aggregatorPath) }
                    .getOrElse {
                        error(
                            "Aggregator project `$aggregatorPath` is not included in settings.gradle.kts."
                        )
                    }

            val paths =
                target.rootProject.leafDescendants().filter { it.name == "impl" }.map { it.path }
            val outputFile = aggregator.projectDir.resolve(AGGREGATOR_OUTPUT_REL_PATH)
            outputFile.parentFile?.mkdirs()
            outputFile.writeText(paths.sorted().joinToString(separator = "\n"))
        }
    }

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
