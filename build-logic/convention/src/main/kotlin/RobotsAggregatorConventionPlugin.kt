import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * Applied to the E2E driver module (`:client:e2e`). Adds every `:robots` module that
 * `convention.robots-aggregator.settings` listed as a `commonMain` `api` dependency, so a test host
 * depending on `:client:e2e` sees every robot type. [AGGREGATOR_OUTPUT_REL_PATH] must match the
 * settings plugin's copy; the two share no classpath.
 */
class RobotsAggregatorConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val inputFile = layout.projectDirectory.file(AGGREGATOR_OUTPUT_REL_PATH)

            val modulesProvider: Provider<List<String>> =
                providers.fileContents(inputFile).asText.map { text ->
                    text.lines().map(String::trim).filter(String::isNotEmpty)
                }

            // Gate on KMP so the `commonMainApi` configuration exists before we add to it,
            // regardless of plugin application order in the consuming script.
            plugins.withId("org.jetbrains.kotlin.multiplatform") {
                dependencies {
                    aggregatedModules(modulesProvider).forEach { path ->
                        add("commonMainApi", project(path))
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
                    "and register the aggregator via `robotsAggregator { aggregator(...) }`.",
            )

    private companion object {
        const val AGGREGATOR_OUTPUT_REL_PATH = "build/robots-aggregator/robots-modules.txt"
    }
}
