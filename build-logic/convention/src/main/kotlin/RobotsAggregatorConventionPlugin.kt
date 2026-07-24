import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * Applied to the E2E host module (`:client:androidApp`). Reads the robots module list that
 * `convention.robots-aggregator.settings` wrote into this project's build directory at settings
 * time and wires each as an `androidTestImplementation` dependency, so a new `:robots` module joins
 * the instrumented suite the moment it is included in settings.gradle.kts.
 *
 * The file is resolved via `layout.projectDirectory`, which is scoped to this project, so no
 * cross-project access occurs. `providers.fileContents` registers the file as a configuration cache
 * input, so changes to the module list invalidate the cache correctly.
 *
 * The path must stay in sync with `RobotsAggregatorSettingsPlugin.AGGREGATOR_OUTPUT_REL_PATH` in
 * the `:build-logic:settings` subproject. Kept separate to avoid a classpath dependency between the
 * settings and convention subprojects.
 */
class RobotsAggregatorConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val inputFile = layout.projectDirectory.file(AGGREGATOR_OUTPUT_REL_PATH)

            val modulesProvider: Provider<List<String>> =
                providers.fileContents(inputFile).asText.map { text ->
                    text.lines().map(String::trim).filter(String::isNotEmpty)
                }

            // Gate on AGP so the `androidTestImplementation` configuration exists before we add to
            // it, regardless of plugin application order in the consuming script.
            plugins.withId("com.android.application") {
                dependencies {
                    aggregatedModules(modulesProvider).forEach { path ->
                        add("androidTestImplementation", project(path))
                    }
                }
            }
        }
    }

    private fun aggregatedModules(provider: Provider<List<String>>): List<String> =
        provider.orNull
            ?: error(
                "Robots module list not found at `$AGGREGATOR_OUTPUT_REL_PATH`. " +
                    "Apply `convention.robots-aggregator.settings` in settings.gradle.kts " +
                    "and register the aggregator via `robotsAggregator { aggregator(...) }`."
            )

    private companion object {
        const val AGGREGATOR_OUTPUT_REL_PATH = "build/robots-aggregator/robots-modules.txt"
    }
}
